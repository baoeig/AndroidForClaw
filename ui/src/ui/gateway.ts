/**
 * Gateway WebSocket Client - OpenClaw aligned
 */

import type {
  GatewayRequest,
  GatewayResponse,
  GatewayEvent,
  GatewayHello
} from './types';

type RequestCallback = {
  resolve: (payload: unknown) => void;
  reject: (error: Error) => void;
  timeout: ReturnType<typeof setTimeout>;
};

export class GatewayClient {
  private ws: WebSocket | null = null;
  private url: string;
  private connected = false;
  private requestId = 0;
  private pendingRequests = new Map<string, RequestCallback>();
  private eventListeners = new Map<string, Set<(payload: unknown) => void>>();
  private heartbeatTimer: number | null = null;
  private heartbeatTimeout: number | null = null;
  private reconnectTimer: number | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;

  // Events
  onConnected?: (hello: GatewayHello) => void;
  onDisconnected?: (reason: string) => void;
  onError?: (error: Error) => void;

  constructor(url?: string) {
    // 自动检测 WebSocket URL
    if (url) {
      this.url = url;
    } else {
      // 通过 Vite proxy 或 ADB 端口转发连接
      const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = location.hostname;
      const port = location.port || '5174';
      this.url = `${protocol}//${host}:${port}/ws`;
    }
  }

  /**
   * 连接到 Gateway
   */
  connect(): Promise<GatewayHello> {
    console.log('🔌 [Gateway] 开始连接...');
    console.log('📍 [Gateway] URL:', this.url);

    return new Promise((resolve, reject) => {
      try {
        console.log('🌐 [Gateway] 创建 WebSocket 连接...');
        this.ws = new WebSocket(this.url);

        this.ws.onopen = () => {
          console.log('✅ [Gateway] WebSocket 连接已建立');
          console.log('📡 [Gateway] 等待 hello 消息...');
          this.connected = true;

          // 启动心跳检测
          this.startHeartbeat();
        };

        this.ws.onmessage = (event) => {
          try {
            console.log('📨 [Gateway] 收到消息:', event.data);
            const frame = JSON.parse(event.data);
            console.log('📦 [Gateway] 解析后的帧:', frame);

            this.handleFrame(frame);

            // First message should be hello
            if (frame.type === 'event' && frame.event === 'hello') {
              console.log('👋 [Gateway] 收到 hello 消息');
              this.connected = true;
              const hello = frame.payload as GatewayHello;
              console.log('✅ [Gateway] 连接成功!');
              console.log('📱 [Gateway] 设备信息:', hello);
              this.onConnected?.(hello);
              resolve(hello);
            }
          } catch (error) {
            console.error('❌ [Gateway] 解析消息失败:', error);
            console.error('📄 [Gateway] 原始数据:', event.data);
          }
        };

        this.ws.onerror = (error) => {
          console.error('❌ [Gateway] WebSocket 错误:', error);
          console.error('🔍 [Gateway] 错误详情:', {
            url: this.url,
            readyState: this.ws?.readyState,
            error
          });
          const err = new Error('WebSocket error');
          this.onError?.(err);
          reject(err);
        };

        this.ws.onclose = (event) => {
          console.log('🔌 [Gateway] WebSocket 关闭');
          console.log('📊 [Gateway] 关闭信息:', {
            code: event.code,
            reason: event.reason,
            wasClean: event.wasClean
          });
          this.connected = false;
          this.ws = null;

          // 停止心跳
          this.stopHeartbeat();

          // Clear pending requests
          console.log('🧹 [Gateway] 清理挂起的请求:', this.pendingRequests.size);
          for (const [id, callback] of this.pendingRequests) {
            clearTimeout(callback.timeout);
            callback.reject(new Error('Connection closed'));
          }
          this.pendingRequests.clear();

          this.onDisconnected?.(event.reason || 'Connection closed');

          // 自动重连
          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.scheduleReconnect();
          }
        };

        // Timeout
        console.log('⏱️  [Gateway] 设置 5 秒连接超时...');
        setTimeout(() => {
          if (!this.connected) {
            console.error('⏰ [Gateway] 连接超时!');
            console.error('🔍 [Gateway] WebSocket 状态:', {
              readyState: this.ws?.readyState,
              url: this.url
            });
            this.ws?.close();
            reject(new Error('Connection timeout'));
          }
        }, 5000);

      } catch (error) {
        console.error('❌ [Gateway] 连接失败:', error);
        reject(error);
      }
    });
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
      this.connected = false;
    }
  }

  /**
   * 发送请求
   */
  async request<T = unknown>(method: string, params?: unknown, timeoutMs = 30000): Promise<T> {
    console.log('📤 [Gateway] 发送请求:', method, params);

    if (!this.connected || !this.ws) {
      console.error('❌ [Gateway] 未连接到 Gateway');
      throw new Error('Not connected to gateway');
    }

    const id = `req-${++this.requestId}`;
    console.log('🆔 [Gateway] 请求 ID:', id);

    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        console.error('⏰ [Gateway] 请求超时:', method);
        this.pendingRequests.delete(id);
        reject(new Error(`Request timeout: ${method}`));
      }, timeoutMs);

      this.pendingRequests.set(id, { resolve, reject, timeout });

      const frame: GatewayRequest = {
        type: 'req',
        id,
        method,
        params
      };

      console.log('📨 [Gateway] 发送帧:', frame);
      this.ws!.send(JSON.stringify(frame));
    });
  }

  /**
   * 监听事件
   */
  on(event: string, callback: (payload: unknown) => void) {
    if (!this.eventListeners.has(event)) {
      this.eventListeners.set(event, new Set());
    }
    this.eventListeners.get(event)!.add(callback);
  }

  /**
   * 取消监听
   */
  off(event: string, callback: (payload: unknown) => void) {
    this.eventListeners.get(event)?.delete(callback);
  }

  /**
   * 处理消息帧
   */
  private handleFrame(frame: GatewayResponse | GatewayEvent) {
    console.log('🔄 [Gateway] 处理帧:', frame.type);

    if (frame.type === 'res') {
      // Response
      console.log('📬 [Gateway] 收到响应:', frame.id);
      const callback = this.pendingRequests.get(frame.id);
      if (callback) {
        clearTimeout(callback.timeout);
        this.pendingRequests.delete(frame.id);

        if (frame.ok) {
          console.log('✅ [Gateway] 请求成功:', frame.id);
          console.log('📦 [Gateway] 响应数据:', frame.payload);
          callback.resolve(frame.payload);
        } else {
          console.error('❌ [Gateway] 请求失败:', frame.id);
          console.error('🔍 [Gateway] 错误信息:', frame.error);
          callback.reject(new Error(frame.error?.message || 'Request failed'));
        }
      } else {
        console.warn('⚠️ [Gateway] 未找到对应的请求回调:', frame.id);
      }
    } else if (frame.type === 'event') {
      // Event
      console.log('📡 [Gateway] 收到事件:', frame.event);
      console.log('📦 [Gateway] 事件数据:', frame.payload);

      const listeners = this.eventListeners.get(frame.event);
      if (listeners) {
        console.log('👂 [Gateway] 触发事件监听器:', listeners.size);
        for (const callback of listeners) {
          try {
            callback(frame.payload);
          } catch (error) {
            console.error(`❌ [Gateway] 事件监听器错误 (${frame.event}):`, error);
          }
        }
      } else {
        console.log('ℹ️  [Gateway] 没有事件监听器:', frame.event);
      }

      // Also trigger wildcard listeners
      const wildcardListeners = this.eventListeners.get('*');
      if (wildcardListeners) {
        for (const callback of wildcardListeners) {
          try {
            callback(frame);
          } catch (error) {
            console.error('❌ [Gateway] 通配符监听器错误:', error);
          }
        }
      }
    } else {
      console.warn('⚠️ [Gateway] 未知的帧类型:', frame);
    }
  }

  /**
   * 检查连接状态
   */
  isConnected(): boolean {
    return this.connected && this.ws !== null && this.ws.readyState === WebSocket.OPEN;
  }

  /**
   * 启动心跳
   */
  private startHeartbeat() {
    this.stopHeartbeat();

    console.log('💓 [Gateway] 启动心跳检测...');

    // 每 25 秒发送一次心跳 (服务端是 30 秒超时)
    this.heartbeatTimer = window.setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        // 发送一个空的 ping 帧 (通过发送一个特殊消息)
        const ping = {
          type: 'ping',
          timestamp: Date.now()
        };
        this.ws.send(JSON.stringify(ping));
        console.log('💓 [Gateway] 发送心跳');

        // 设置超时检测
        this.heartbeatTimeout = window.setTimeout(() => {
          console.warn('⚠️ [Gateway] 心跳超时，关闭连接');
          this.ws?.close();
        }, 10000); // 10 秒超时
      }
    }, 25000);
  }

  /**
   * 停止心跳
   */
  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
    if (this.heartbeatTimeout) {
      clearTimeout(this.heartbeatTimeout);
      this.heartbeatTimeout = null;
    }
    console.log('💓 [Gateway] 心跳已停止');
  }

  /**
   * 调度重连
   */
  private scheduleReconnect() {
    if (this.reconnectTimer) {
      return;
    }

    this.reconnectAttempts++;
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts - 1), 30000);
    console.log(`🔄 [Gateway] 将在 ${delay}ms 后重连 (尝试 ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null;
      console.log('🔄 [Gateway] 开始重连...');
      this.connect().catch(error => {
        console.error('❌ [Gateway] 重连失败:', error);
      });
    }, delay);
  }
}
