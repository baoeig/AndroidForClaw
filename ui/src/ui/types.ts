/**
 * Type Definitions - OpenClaw aligned
 */

// ===== Chat Types =====

export type ChatMessage = {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
  toolCalls?: ToolCall[];
  status?: 'sending' | 'sent' | 'error';
};

export type ToolCall = {
  id: string;
  name: string;
  args: Record<string, unknown>;
  result?: ToolResult;
  timestamp: number;
};

export type ToolResult = {
  success: boolean;
  content: string;
  metadata?: Record<string, unknown>;
};

// ===== Session Types =====

export type Session = {
  id: string;
  title: string;
  createdAt: number;
  messages: ChatMessage[];
  isActive: boolean;
};

// ===== Device Types =====

export type DeviceStatus = {
  connected: boolean;
  deviceId: string;
  deviceModel: string;
  androidVersion: string;
  apiLevel: number;
  architecture: string;
  permissions: {
    accessibility: boolean;
    overlay: boolean;
    mediaProjection: boolean;
  };
  lastInboundAt?: number;
  lastOutboundAt?: number;
  lastError?: string;
};

// ===== Channel Types =====

export type ChannelAccount = {
  accountId: string;
  name?: string;
  enabled: boolean;
  connected: boolean;
  running: boolean;
  deviceId?: string;
  deviceModel?: string;
  androidVersion?: string;
  apiLevel?: number;
  accessibilityEnabled: boolean;
  overlayPermission: boolean;
  mediaProjection: boolean;
};

export type ChannelStatus = {
  timestamp: number;
  channelId: string;
  accounts: ChannelAccount[];
  defaultAccountId?: string;
};

// ===== Gateway Types =====

export type GatewayRequest = {
  type: 'req';
  id: string;
  method: string;
  params?: unknown;
};

export type GatewayResponse = {
  type: 'res';
  id: string;
  ok: boolean;
  payload?: unknown;
  error?: {
    code: string;
    message: string;
    details?: unknown;
  };
};

export type GatewayEvent = {
  type: 'event';
  event: string;
  payload?: unknown;
  seq?: number;
};

export type GatewayHello = {
  version: string;
  agent: string;
  channel: string;
  deviceId: string;
  capabilities: string[];
};

// ===== UI State Types =====

export type Tab = 'chat' | 'device' | 'screenshot' | 'sessions' | 'settings';

export type ThemeMode = 'light' | 'dark' | 'system';

export type AppState = {
  // Connection
  connected: boolean;
  hello: GatewayHello | null;
  lastError: string | null;

  // Navigation
  currentTab: Tab;
  sessionId: string | null;

  // Chat
  chatMessages: ChatMessage[];
  chatLoading: boolean;
  chatRunId: string | null;

  // Device
  deviceStatus: DeviceStatus | null;
  deviceLoading: boolean;

  // Sessions
  sessions: Session[];
  sessionsLoading: boolean;

  // Screenshot
  latestScreenshot: string | null;  // base64 image data
  screenshotLoading: boolean;

  // Channel
  channelStatus: ChannelStatus | null;
  channelLoading: boolean;

  // UI Settings
  theme: ThemeMode;
  sidebarCollapsed: boolean;
};

// ===== API Types =====

export type ApiChatRequest = {
  sessionId?: string;
  message: string;
  attachments?: ChatAttachment[];
};

export type ChatAttachment = {
  type: 'image' | 'file';
  data: string;  // base64
  mimeType: string;
  name?: string;
};

export type ApiChatResponse = {
  runId: string;
  sessionId: string;
};

export type ApiScreenshotRequest = {
  width?: number;
  height?: number;
  quality?: number;
};

export type ApiScreenshotResponse = {
  data: string;  // base64
  width: number;
  height: number;
  timestamp: number;
};

export type ApiDeviceControlRequest = {
  action: 'tap' | 'swipe' | 'type' | 'home' | 'back' | 'open_app';
  params: Record<string, unknown>;
};

export type ApiDeviceControlResponse = {
  success: boolean;
  message?: string;
};
