/**
 * Chat View - 聊天界面
 */

import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { ChatMessage } from '../types';

@customElement('chat-view')
export class ChatView extends LitElement {
  static styles = css`
    :host {
      display: flex;
      flex-direction: column;
      height: 100%;
      background: var(--bg-primary);
    }

    .chat-container {
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .chat-messages {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .message {
      display: flex;
      flex-direction: column;
      max-width: 70%;
      padding: 12px 16px;
      border-radius: 12px;
      word-wrap: break-word;
    }

    .message.user {
      align-self: flex-end;
      background: var(--color-primary);
      color: white;
    }

    .message.assistant {
      align-self: flex-start;
      background: var(--bg-secondary);
      color: var(--text-primary);
      border: 1px solid var(--border-color);
    }

    .message-role {
      font-size: 12px;
      font-weight: 600;
      margin-bottom: 4px;
      opacity: 0.7;
    }

    .message-content {
      font-size: 14px;
      line-height: 1.5;
      white-space: pre-wrap;
    }

    .message-timestamp {
      font-size: 11px;
      margin-top: 4px;
      opacity: 0.6;
    }

    .tool-calls {
      margin-top: 8px;
      padding: 8px;
      background: var(--bg-tertiary);
      border-radius: 6px;
      font-size: 13px;
    }

    .tool-call {
      margin-top: 4px;
      padding: 4px 8px;
      background: var(--bg-primary);
      border-radius: 4px;
      font-family: monospace;
    }

    .chat-input {
      display: flex;
      padding: 16px;
      background: var(--bg-secondary);
      border-top: 1px solid var(--border-color);
      gap: 12px;
    }

    .input-field {
      flex: 1;
      padding: 12px 16px;
      background: var(--bg-tertiary);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      color: var(--text-primary);
      font-size: 14px;
      resize: vertical;
      min-height: 48px;
      max-height: 200px;
      font-family: inherit;
    }

    .input-field:focus {
      outline: none;
      border-color: var(--color-primary);
    }

    .input-field::placeholder {
      color: var(--text-tertiary);
    }

    .send-button {
      padding: 12px 24px;
      background: var(--color-primary);
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;
      white-space: nowrap;
    }

    .send-button:hover {
      background: var(--color-primary-hover);
    }

    .send-button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      color: var(--text-secondary);
      gap: 16px;
    }

    .empty-icon {
      font-size: 64px;
    }

    .empty-text {
      font-size: 16px;
    }

    .loading-indicator {
      padding: 12px;
      text-align: center;
      color: var(--text-secondary);
      font-size: 14px;
    }
  `;

  @property({ type: Array }) messages: ChatMessage[] = [];
  @property({ type: Boolean }) isLoading = false;
  @property({ type: Function }) onSendMessage?: (message: string) => void;

  private inputValue = '';

  private handleInput(e: Event) {
    const textarea = e.target as HTMLTextAreaElement;
    this.inputValue = textarea.value;
  }

  private handleKeyDown(e: KeyboardEvent) {
    // Ctrl/Cmd + Enter to send
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault();
      this.sendMessage();
    }
  }

  private sendMessage() {
    if (!this.inputValue.trim() || this.isLoading) {
      return;
    }

    console.log('📤 [Chat] 发送消息:', this.inputValue);

    if (this.onSendMessage) {
      this.onSendMessage(this.inputValue);
      this.inputValue = '';

      // Clear input field
      const textarea = this.shadowRoot?.querySelector('.input-field') as HTMLTextAreaElement;
      if (textarea) {
        textarea.value = '';
      }

      // Scroll to bottom
      setTimeout(() => this.scrollToBottom(), 100);
    }
  }

  private scrollToBottom() {
    const messagesContainer = this.shadowRoot?.querySelector('.chat-messages');
    if (messagesContainer) {
      messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
  }

  private formatTimestamp(timestamp: number): string {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  updated() {
    // Auto scroll to bottom when new messages arrive
    if (this.messages.length > 0) {
      this.scrollToBottom();
    }
  }

  render() {
    console.log('🎨 [Chat] 渲染视图, 消息数:', this.messages.length);

    return html`
      <div class="chat-container">
        <div class="chat-messages">
          ${this.messages.length === 0 ? this.renderEmptyState() : this.renderMessages()}
          ${this.isLoading ? html`
            <div class="loading-indicator">
              <div class="loading"></div>
              <div>正在处理...</div>
            </div>
          ` : ''}
        </div>
        ${this.renderInput()}
      </div>
    `;
  }

  private renderEmptyState() {
    return html`
      <div class="empty-state">
        <div class="empty-icon">💬</div>
        <div class="empty-text">开始对话</div>
        <div style="font-size: 14px; color: var(--text-tertiary);">
          发送消息给 PhoneForClaw AI 助手
        </div>
      </div>
    `;
  }

  private renderMessages() {
    return html`
      ${this.messages.map(message => html`
        <div class="message ${message.role}">
          <div class="message-role">
            ${message.role === 'user' ? '👤 你' : '🤖 AI'}
          </div>
          <div class="message-content">${message.content}</div>
          <div class="message-timestamp">
            ${this.formatTimestamp(message.timestamp)}
          </div>
          ${message.toolCalls && message.toolCalls.length > 0 ? this.renderToolCalls(message.toolCalls) : ''}
        </div>
      `)}
    `;
  }

  private renderToolCalls(toolCalls: any[]) {
    return html`
      <div class="tool-calls">
        <div style="font-weight: 600; margin-bottom: 4px;">🛠️ 工具调用:</div>
        ${toolCalls.map(tool => html`
          <div class="tool-call">
            ${tool.name}(${JSON.stringify(tool.args)})
          </div>
        `)}
      </div>
    `;
  }

  private renderInput() {
    return html`
      <div class="chat-input">
        <textarea
          class="input-field"
          placeholder="输入消息... (Ctrl+Enter 发送)"
          @input=${this.handleInput}
          @keydown=${this.handleKeyDown}
          ?disabled=${this.isLoading}
        ></textarea>
        <button
          class="send-button"
          @click=${this.sendMessage}
          ?disabled=${this.isLoading || !this.inputValue.trim()}
        >
          ${this.isLoading ? '处理中...' : '发送'}
        </button>
      </div>
    `;
  }
}
