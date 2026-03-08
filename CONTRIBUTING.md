# Contributing to phoneforclaw

感谢你对 phoneforclaw 项目的关注！我们欢迎任何形式的贡献。

## 🤝 如何贡献

### 报告 Bug

如果你发现了 bug，请在 [GitHub Issues](https://github.com/xiaomo/phoneforclaw/issues) 中创建一个新 issue，包含：

- Bug 描述
- 复现步骤
- 期望行为
- 实际行为
- 设备和系统信息
- 日志（如果有）

### 提出新功能

如果你有新功能建议，请先创建一个 issue 讨论：

- 功能描述
- 使用场景
- 预期收益
- 可能的实现方案

### 提交代码

1. **Fork 项目**
   ```bash
   # Fork 项目到你的账号
   # 克隆你的 fork
   git clone https://github.com/YOUR_USERNAME/phoneforclaw.git
   cd phoneforclaw
   ```

2. **创建分支**
   ```bash
   git checkout -b feature/your-feature-name
   # 或
   git checkout -b fix/your-bug-fix
   ```

3. **编写代码**
   - 遵循项目代码风格（Kotlin 官方规范）
   - 添加必要的注释
   - 保持代码简洁清晰

4. **测试**
   ```bash
   ./gradlew test
   ./gradlew lint
   ```

5. **提交**
   ```bash
   git add .
   git commit -m "feat: 添加新功能描述"
   # 或
   git commit -m "fix: 修复 bug 描述"
   ```

   提交信息格式：
   - `feat:` 新功能
   - `fix:` Bug 修复
   - `docs:` 文档更新
   - `style:` 代码格式（不影响功能）
   - `refactor:` 重构
   - `test:` 测试相关
   - `chore:` 构建工具或辅助工具的变动

6. **Push 并创建 PR**
   ```bash
   git push origin feature/your-feature-name
   ```
   
   然后在 GitHub 上创建 Pull Request。

## 📖 开发规范

### 代码风格

- 遵循 Kotlin 官方编码规范
- 使用有意义的变量和函数名
- 添加必要的注释，特别是复杂逻辑
- 保持函数简短，单一职责

### 架构原则

参考 [CLAUDE.md](./CLAUDE.md) 了解项目架构：

1. **Knowledge & Code Separation** - Tools 提供能力，Skills 教学
2. **Agent Loop** - LLM → Tool Call → Observation 循环
3. **Skills System** - Markdown 文档传递知识
4. **Gateway Architecture** - 多渠道接入与远程控制

### 添加新工具

1. 在 `app/src/main/java/com/xiaomo/androidforclaw/agent/tools/` 创建工具类
2. 实现 `Skill` 接口
3. 在 `SkillRegistry.kt` 中注册
4. 添加对应的 Skill 文档（可选）

示例：
```kotlin
class YourSkill : Skill {
    override val name = "your_skill"
    override val description = "Your skill description"
    
    override fun getToolDefinition(): ToolDefinition { ... }
    override suspend fun execute(args: Map<String, Any?>): SkillResult { ... }
}
```

### 添加新 Skill

1. 在 `app/src/main/assets/skills/` 创建 Markdown 文件
2. 遵循 AgentSkills.io 格式：
   ```markdown
   ---
   name: skill-name
   description: Skill description
   metadata: { ... }
   ---
   
   # Skill Content
   ```

## 🧪 测试

- 单元测试：`./gradlew test`
- Lint 检查：`./gradlew lint`
- 手动测试：在真机或模拟器上安装测试

## 📝 文档

- 更新代码时，同步更新相关文档
- 新功能需要添加文档说明
- 保持 README.md 和 CLAUDE.md 的准确性

## 🎓 学习资源

- [CLAUDE.md](./CLAUDE.md) - 开发指南
- [ARCHITECTURE.md](./ARCHITECTURE.md) - 架构设计
- [OpenClaw](https://github.com/openclaw/openclaw) - 架构灵感来源

## 💬 联系我们

- GitHub Issues: [项目 Issues](https://github.com/xiaomo/phoneforclaw/issues)
- Discussions: [项目讨论区](https://github.com/xiaomo/phoneforclaw/discussions)

## 📜 行为准则

请遵守我们的 [行为准则](./CODE_OF_CONDUCT.md)，保持友好和尊重。

---

再次感谢你的贡献！ 🙏
