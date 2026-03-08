/**
 * Main App Component - OpenClaw aligned
 */

import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { GatewayClient } from './gateway';
import './views/chat';
import type {
  AppState,
  Tab,
  ThemeMode,
  ChatMessage,
  DeviceStatus,
  Session,
  ChannelStatus,
  GatewayHello
} from './types';

@customElement('phoneforclaw-app')
export class PhoneForClawApp extends LitElement {
  static styles = css`
    :host {
      display: block;
      width: 100%;
      height: 100vh;
      background: var(--bg-primary);
      color: var(--text-primary);
    }

    .app-container {
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .app-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px 24px;
      background: var(--bg-secondary);
      border-bottom: 1px solid var(--border-color);
    }

    .app-title {
      font-size: 20px;
      font-weight: 600;
    }

    .connection-status {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
    }

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }

    .status-dot.connected {
      background: var(--color-success);
    }

    .status-dot.disconnected {
      background: var(--color-error);
    }

    .app-content {
      display: flex;
      flex: 1;
      overflow: hidden;
    }

    .app-sidebar {
      width: 240px;
      background: var(--bg-secondary);
      border-right: 1px solid var(--border-color);
      display: flex;
      flex-direction: column;
    }

    .app-main {
      flex: 1;
      overflow: auto;
    }

    .nav-tabs {
      display: flex;
      flex-direction: column;
      padding: 16px 8px;
      gap: 4px;
    }

    .nav-tab {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      border-radius: 8px;
      cursor: pointer;
      transition: background 0.2s;
      font-size: 14px;
    }

    .nav-tab:hover {
      background: var(--bg-hover);
    }

    .nav-tab.active {
      background: var(--bg-active);
      color: var(--color-primary);
      font-weight: 500;
    }

    .nav-icon {
      font-size: 18px;
    }

    .loading-screen {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      gap: 16px;
    }

    .error-screen {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      gap: 16px;
      padding: 32px;
      text-align: center;
    }

    .error-message {
      color: var(--color-error);
      font-size: 16px;
    }

    .retry-button {
      padding: 12px 24px;
      background: var(--color-primary);
      color: white;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      font-size: 14px;
      font-weight: 500;
    }

    .retry-button:hover {
      background: var(--color-primary-hover);
    }
  `;

  @state() private state: AppState = {
    connected: false,
    hello: null,
    lastError: null,
    currentTab: 'chat',
    sessionId: null,
    chatMessages: [],
    chatLoading: false,
    chatRunId: null,
    deviceStatus: null,
    deviceLoading: false,
    sessions: [],
    sessionsLoading: false,
    latestScreenshot: null,
    screenshotLoading: false,
    channelStatus: null,
    channelLoading: false,
    theme: 'dark',
    sidebarCollapsed: false
  };

  private gateway: GatewayClient | null = null;

  connectedCallback() {
    super.connectedCallback();
    console.log('🎬 [App] 组件已挂载，开始初始化...');
    this.initializeGateway();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this.gateway?.disconnect();
  }

  private async initializeGateway() {
    console.log('🚀 [App] 初始化 Gateway...');

    try {
      // Parse URL parameters for custom Gateway IP
      const urlParams = new URLSearchParams(window.location.search);
      const customIp = urlParams.get('ip') || urlParams.get('gateway');

      let gatewayUrl: string | undefined;
      if (customIp) {
        // User specified custom IP
        const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
        // Support both "192.168.1.100" and "192.168.1.100:8080" formats
        const [host, port] = customIp.includes(':') ? customIp.split(':') : [customIp, '8080'];
        gatewayUrl = `${protocol}//${host}:${port}/`;
        console.log('🔧 [App] 使用自定义 Gateway IP:', gatewayUrl);
      }

      console.log('🔧 [App] 创建 GatewayClient 实例');
      this.gateway = new GatewayClient(gatewayUrl);

      this.gateway.onConnected = (hello: GatewayHello) => {
        console.log('✅ [App] Gateway 连接成功!');
        console.log('👋 [App] Hello 消息:', hello);
        this.state = {
          ...this.state,
          connected: true,
          hello,
          lastError: null
        };
        console.log('📊 [App] 更新状态: connected = true');
        console.log('📥 [App] 开始加载初始数据...');
        this.loadInitialData();
      };

      this.gateway.onDisconnected = (reason: string) => {
        console.log('🔌 [App] Gateway 断开连接:', reason);
        this.state = {
          ...this.state,
          connected: false,
          lastError: reason
        };
        console.log('📊 [App] 更新状态: connected = false');
      };

      this.gateway.onError = (error: Error) => {
        console.error('❌ [App] Gateway 错误:', error);
        this.state = {
          ...this.state,
          lastError: error.message
        };
        console.log('📊 [App] 更新状态: lastError =', error.message);
      };

      // Listen to events
      console.log('👂 [App] 注册事件监听器...');

      this.gateway.on('chat.message', (payload) => {
        console.log('💬 [App] 收到聊天消息:', payload);
        this.handleChatMessage(payload as ChatMessage);
      });

      this.gateway.on('device.status', (payload) => {
        console.log('📱 [App] 收到设备状态:', payload);
        this.state = {
          ...this.state,
          deviceStatus: payload as DeviceStatus
        };
      });

      this.gateway.on('channel.status', (payload) => {
        console.log('📡 [App] 收到频道状态:', payload);
        this.state = {
          ...this.state,
          channelStatus: payload as ChannelStatus
        };
      });

      console.log('🔌 [App] 开始连接到 Gateway...');
      await this.gateway.connect();
      console.log('✅ [App] Gateway 连接流程完成');

    } catch (error) {
      console.error('❌ [App] Gateway 初始化失败:', error);
      console.error('🔍 [App] 错误详情:', {
        message: error instanceof Error ? error.message : 'Unknown error',
        stack: error instanceof Error ? error.stack : undefined
      });
      this.state = {
        ...this.state,
        connected: false,
        lastError: error instanceof Error ? error.message : 'Connection failed'
      };
      console.log('📊 [App] 更新状态: 连接失败');
    }
  }

  private async loadInitialData() {
    console.log('📥 [App] 加载初始数据...');

    try {
      // Load device status
      console.log('📱 [App] 请求设备状态...');
      const deviceStatus = await this.gateway!.request<DeviceStatus>('device.status');
      console.log('✅ [App] 设备状态加载成功:', deviceStatus);
      this.state = { ...this.state, deviceStatus };

      // Load channel status
      console.log('📡 [App] 请求频道状态...');
      const channelStatus = await this.gateway!.request<ChannelStatus>('channel.status');
      console.log('✅ [App] 频道状态加载成功:', channelStatus);
      this.state = { ...this.state, channelStatus };

      // Load sessions
      console.log('📋 [App] 请求会话列表...');
      const sessions = await this.gateway!.request<Session[]>('sessions.list');
      console.log('✅ [App] 会话列表加载成功:', sessions);
      this.state = { ...this.state, sessions };

      // Load chat history for default session
      console.log('💬 [App] 加载会话历史...');
      const historyResponse = await this.gateway!.request<any>('sessions.history', {
        sessionId: 'default'
      });
      console.log('✅ [App] 会话历史加载成功:', historyResponse);

      // Convert to ChatMessage format
      const chatMessages: ChatMessage[] = historyResponse.messages.map((msg: any, index: number) => ({
        id: `msg-${Date.now()}-${index}`,
        role: msg.role,
        content: msg.content,
        timestamp: Date.now()
      }));

      this.state = { ...this.state, chatMessages };
      console.log('📝 [App] 加载了 ' + chatMessages.length + ' 条历史消息');

      console.log('🎉 [App] 所有初始数据加载完成!');

    } catch (error) {
      console.error('❌ [App] 加载初始数据失败:', error);
    }
  }

  private handleChatMessage(message: ChatMessage) {
    this.state = {
      ...this.state,
      chatMessages: [...this.state.chatMessages, message]
    };
  }

  private switchTab(tab: Tab) {
    this.state = {
      ...this.state,
      currentTab: tab
    };
  }

  private retryConnection() {
    this.state = {
      ...this.state,
      lastError: null
    };
    this.initializeGateway();
  }

  render() {
    if (this.state.lastError && !this.state.connected) {
      return this.renderError();
    }

    if (!this.state.connected) {
      return this.renderLoading();
    }

    return html`
      <div class="app-container">
        ${this.renderHeader()}
        <div class="app-content">
          ${this.renderSidebar()}
          <div class="app-main">
            ${this.renderMainContent()}
          </div>
        </div>
      </div>
    `;
  }

  private renderHeader() {
    return html`
      <div class="app-header">
        <div class="app-title">
          📱 PhoneForClaw Control
        </div>
        <div class="connection-status">
          <span class="status-dot ${this.state.connected ? 'connected' : 'disconnected'}"></span>
          <span>${this.state.connected ? '已连接' : '未连接'}</span>
          ${this.state.hello ? html`
            <span style="color: var(--text-secondary); font-size: 12px;">
              ${this.state.hello.deviceId.slice(0, 8)}...
            </span>
          ` : ''}
        </div>
      </div>
    `;
  }

  private renderSidebar() {
    const tabs: Array<{ id: Tab; icon: string; label: string }> = [
      { id: 'chat', icon: '💬', label: '对话' },
      { id: 'device', icon: '📱', label: '设备' },
      { id: 'screenshot', icon: '📸', label: '截图' },
      { id: 'sessions', icon: '📋', label: '会话' },
      { id: 'settings', icon: '⚙️', label: '设置' }
    ];

    return html`
      <div class="app-sidebar">
        <div class="nav-tabs">
          ${tabs.map(tab => html`
            <div
              class="nav-tab ${this.state.currentTab === tab.id ? 'active' : ''}"
              @click=${() => this.switchTab(tab.id)}
            >
              <span class="nav-icon">${tab.icon}</span>
              <span>${tab.label}</span>
            </div>
          `)}
        </div>
      </div>
    `;
  }

  private renderMainContent() {
    switch (this.state.currentTab) {
      case 'chat':
        return this.renderChatView();
      case 'device':
        return this.renderDeviceView();
      case 'screenshot':
        return html`<div style="padding: 24px;">截图视图 (开发中)</div>`;
      case 'sessions':
        return html`<div style="padding: 24px;">会话管理视图 (开发中)</div>`;
      case 'settings':
        return html`<div style="padding: 24px;">设置视图 (开发中)</div>`;
      default:
        return html`<div style="padding: 24px;">未知视图</div>`;
    }
  }

  private renderChatView() {
    console.log('🎨 [App] 渲染聊天视图');
    return html`
      <chat-view
        .messages=${this.state.chatMessages}
        .isLoading=${this.state.chatLoading}
        .onSendMessage=${(message: string) => this.sendChatMessage(message)}
      ></chat-view>
    `;
  }

  private renderDeviceView() {
    const device = this.state.deviceStatus;
    if (!device) {
      return html`
        <div style="padding: 24px;">
          <div style="text-align: center; color: var(--text-secondary);">
            加载设备信息...
          </div>
        </div>
      `;
    }

    return html`
      <div style="padding: 24px;">
        <h2>设备信息</h2>
        <div style="margin-top: 16px;">
          <div><strong>设备 ID:</strong> ${device.deviceId}</div>
          <div><strong>设备型号:</strong> ${device.deviceModel}</div>
          <div><strong>Android 版本:</strong> ${device.androidVersion} (API ${device.apiLevel})</div>
          <div><strong>架构:</strong> ${device.architecture}</div>
          <div style="margin-top: 16px;">
            <strong>权限状态:</strong>
            <div style="margin-left: 20px;">
              <div>无障碍: ${device.permissions.accessibility ? '✓ 已授权' : '✗ 未授权'}</div>
              <div>悬浮窗: ${device.permissions.overlay ? '✓ 已授权' : '✗ 未授权'}</div>
              <div>录屏: ${device.permissions.mediaProjection ? '✓ 已授权' : '✗ 未授权'}</div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  private async sendChatMessage(message: string) {
    console.log('💬 [App] 发送聊天消息:', message);

    // Add user message
    const userMessage: ChatMessage = {
      id: `msg-${Date.now()}`,
      role: 'user',
      content: message,
      timestamp: Date.now(),
      status: 'sent'
    };

    this.state = {
      ...this.state,
      chatMessages: [...this.state.chatMessages, userMessage],
      chatLoading: true
    };

    try {
      // Send to gateway
      console.log('📤 [App] 调用 Gateway chat API...');
      const result = await this.gateway!.request('chat.send', {
        sessionId: this.state.sessionId,
        message
      });

      console.log('✅ [App] Chat API 响应:', result);

      // Response will come via event listener
      // For now, add a placeholder assistant message
      const assistantMessage: ChatMessage = {
        id: `msg-${Date.now()}-assistant`,
        role: 'assistant',
        content: '收到消息，正在处理...',
        timestamp: Date.now()
      };

      this.state = {
        ...this.state,
        chatMessages: [...this.state.chatMessages, assistantMessage],
        chatLoading: false
      };

    } catch (error) {
      console.error('❌ [App] 发送消息失败:', error);

      const errorMessage: ChatMessage = {
        id: `msg-${Date.now()}-error`,
        role: 'assistant',
        content: `错误: ${error instanceof Error ? error.message : '发送失败'}`,
        timestamp: Date.now(),
        status: 'error'
      };

      this.state = {
        ...this.state,
        chatMessages: [...this.state.chatMessages, errorMessage],
        chatLoading: false
      };
    }
  }

  private renderLoading() {
    return html`
      <div class="loading-screen">
        <div style="font-size: 48px;">📱</div>
        <div>正在连接到 PhoneForClaw...</div>
      </div>
    `;
  }

  private renderError() {
    return html`
      <div class="error-screen">
        <div style="font-size: 48px;">⚠️</div>
        <div class="error-message">
          连接失败: ${this.state.lastError}
        </div>
        <button class="retry-button" @click=${this.retryConnection}>
          重试连接
        </button>
      </div>
    `;
  }
}
