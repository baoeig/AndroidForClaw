# Frequently Asked Questions (FAQ)

---

## General Questions

### Q: What is BrowserForClaw?
**A:** BrowserForClaw is a browser automation system for AI agents on Android. It has two parts:
1. An Android app that provides browser control via HTTP API
2. A SKILL.md file that teaches AI agents how to use it

See [UNDERSTANDING.md](./UNDERSTANDING.md) for details.

---

### Q: Do I need to be a developer to use this?
**A:** It depends:
- **Just want to use the app**: Basic knowledge of Android and command line (adb)
- **Integrate with AI agent**: Need to understand HTTP APIs and your agent framework
- **Contribute code**: Need Kotlin/Android development skills

---

### Q: What's the difference between BrowserForClaw and Selenium/Puppeteer?
**A:**
- **Selenium/Puppeteer**: Desktop/server browser automation, complex setup, heavy
- **BrowserForClaw**: Mobile-first, HTTP API, lightweight, AI-agent-optimized

BrowserForClaw is specifically designed for AI agents running on mobile devices.

---

## Installation & Setup

### Q: Where do I get the APK?
**A:** Two options:
1. Download from [GitHub Releases](https://github.com/your-username/BrowserForClaw/releases)
2. Build from source (requires einkbro project)

---

### Q: Why is the app called "EinkBro"?
**A:** BrowserForClaw is built on top of the [EinkBro browser](https://github.com/plateaukao/einkbro). The app name reflects its foundation, but includes BrowserForClaw automation capabilities.

---

### Q: Do I need to install anything else besides the APK?
**A:** No. The HTTP server and all 13 browser tools are built into the APK.

For development:
- Android device/emulator
- ADB for installation and port forwarding
- (Optional) An AI agent system if you want AI-powered automation

---

### Q: Can I use this on iOS?
**A:** No, BrowserForClaw is Android-only. The architecture relies on Android WebView and system APIs.

---

## Usage

### Q: Can I use BrowserForClaw without an AI agent?
**A:** Yes! You can send HTTP requests directly using:
- `curl` from command line
- Postman or similar tools
- Any programming language with HTTP support (Python, JavaScript, Java, etc.)

The SKILL.md file is optional—it's only needed if you want an AI agent to use the browser intelligently.

---

### Q: What's the point of SKILL.md?
**A:** Without SKILL.md:
```python
# You manually code every step
client.navigate("google.com")
time.sleep(3)  # How long to wait?
client.type("#search", "query")  # What's the selector?
```

With SKILL.md:
```python
# AI agent automatically:
agent.ask("Search Google for AI news")
# - Loads skill knowledge
# - Knows the right sequence
# - Handles errors intelligently
# - Returns formatted results
```

SKILL.md makes the AI agent smart about browser automation.

---

### Q: Which AI agents support SKILL.md?
**A:** Any agent framework that supports AgentSkills.io format:
- OpenClaw
- phoneforclaw
- Custom agents (you can parse SKILL.md yourself)

---

### Q: Can I use BrowserForClaw with Python/JavaScript/other languages?
**A:** Yes! BrowserForClaw uses HTTP API, so any language can call it:

```python
# Python example
import requests
requests.post("http://localhost:8765/api/browser/execute", json={
    "tool": "browser_navigate",
    "args": {"url": "https://example.com"}
})
```

```javascript
// JavaScript example
fetch("http://localhost:8765/api/browser/execute", {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify({
        tool: "browser_navigate",
        args: {url: "https://example.com"}
    })
});
```

---

## Technical Questions

### Q: Why port 8765?
**A:** Standard HTTP alternative port. You can change it in the code if needed, but 8765 is widely compatible and easy to remember.

---

### Q: Can I run multiple instances?
**A:** Each Android device can run one instance. If you have multiple devices, each can run BrowserForClaw on port 8765 (isolated by device).

---

### Q: Is the HTTP server secure?
**A:** Currently, it's designed for **local development** (localhost) or **trusted networks**. There's no authentication by default.

For production use, you should:
- Add API key authentication
- Use only on trusted networks
- Consider VPN for remote access

---

### Q: What happens if the app crashes?
**A:** The HTTP server runs within the app, so if the app crashes, the server stops. You'll need to restart the app:
```bash
adb shell am start -n info.plateaukao.einkbro/.activity.BrowserActivity
```

---

### Q: Can I use this in headless mode (no UI)?
**A:** Not currently. The Android WebView requires a UI context to render pages. However, the app can run in the background once started.

---

### Q: What about JavaScript-heavy sites (SPAs)?
**A:** BrowserForClaw uses real WebView, so JavaScript works. Use the `browser_wait` tool to ensure elements load:
```json
{
  "tool": "browser_wait",
  "args": {"selector": ".loaded-content", "timeMs": 10000}
}
```

---

### Q: Can I execute custom JavaScript?
**A:** Yes! Use `browser_execute`:
```bash
curl -X POST http://localhost:8765/api/browser/execute \
  -H "Content-Type: application/json" \
  -d '{"tool":"browser_execute","args":{"script":"return document.title"}}'
```

---

## Troubleshooting

### Q: "Connection refused" error
**A:** Check:
1. Is the app running? `adb shell ps | grep einkbro`
2. Is port forwarding set? `adb forward tcp:8765 tcp:8765`
3. Try: `curl http://localhost:8765/health`

---

### Q: "Element not found" error
**A:** Common causes:
1. Page not fully loaded → Use `browser_wait`
2. Wrong CSS selector → Inspect page to find correct selector
3. Element in iframe → JavaScript execution may be needed

---

### Q: Browser is slow to respond
**A:** Possible reasons:
1. Slow network → Normal for page loads
2. Heavy JavaScript → Wait longer with `browser_wait`
3. Multiple rapid requests → Add small delays between requests

---

### Q: How do I debug what the browser is doing?
**A:** Check logs:
```bash
adb logcat | grep -E "(BrowserHttpServer|BrowserTools)"
```

Or use `browser_screenshot` to see what's on screen.

---

## Development & Contributing

### Q: How do I build from source?
**A:** BrowserForClaw is part of the einkbro project:
```bash
cd /path/to/einkbro
./gradlew assembleDebug
# APK output: app/build/outputs/apk/debug/
```

See [CONTRIBUTING.md](./CONTRIBUTING.md) for details.

---

### Q: Can I add new browser tools?
**A:** Yes! Follow these steps:
1. Create `BrowserNewTool.kt` in `tools/` directory
2. Implement the `BrowserTool` interface
3. Register in `BrowserToolsExecutor`
4. Update documentation (TOOLS_REFERENCE.md, SKILL.md)

See [CONTRIBUTING.md](./CONTRIBUTING.md) for examples.

---

### Q: Where should I report bugs?
**A:** [GitHub Issues](https://github.com/your-username/BrowserForClaw/issues) with:
- Clear description
- Steps to reproduce
- Device/Android version
- Logs from `adb logcat`

---

### Q: Can I contribute?
**A:** Yes! Contributions welcome:
- Bug fixes
- New tools
- Documentation improvements
- Test coverage
- Client libraries (Python, JS, etc.)

See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

---

## Licensing & Use

### Q: What's the license?
**A:** MIT License. You can:
- Use commercially
- Modify
- Distribute
- Private use

See [LICENSE](./LICENSE) for full text.

---

### Q: Can I use this in my commercial product?
**A:** Yes, MIT License allows commercial use. No attribution required (but appreciated!).

---

### Q: Is EinkBro's license compatible?
**A:** Yes, EinkBro is also open source. Check their license for details.

---

## Comparison with Other Tools

### Q: BrowserForClaw vs Appium?
**A:**
- **Appium**: Full mobile app automation (not just browser)
- **BrowserForClaw**: Browser-specific, simpler API, AI-agent-optimized

Use Appium for native app testing, BrowserForClaw for web automation.

---

### Q: BrowserForClaw vs ChromeDriver?
**A:**
- **ChromeDriver**: Desktop Chrome automation
- **BrowserForClaw**: Android WebView, HTTP API, mobile-first

---

## Future Plans

### Q: Will there be an iOS version?
**A:** Not currently planned. iOS has different restrictions and architecture. Contributions welcome if someone wants to port it!

---

### Q: Will you add authentication?
**A:** Planned for future versions. Current focus is on core functionality.

---

### Q: Can I sponsor development?
**A:** Not set up yet, but if there's demand, we can add sponsor options.

---

## Still Have Questions?

- 📖 Read [UNDERSTANDING.md](./UNDERSTANDING.md) for architecture details
- 🚀 Try [QUICK_START.md](./QUICK_START.md) for hands-on experience
- 💬 Ask in [GitHub Discussions](https://github.com/your-username/BrowserForClaw/discussions)
- 🐛 Report bugs in [GitHub Issues](https://github.com/your-username/BrowserForClaw/issues)

---

**Last Updated:** 2026-03-06
