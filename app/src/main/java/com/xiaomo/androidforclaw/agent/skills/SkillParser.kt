package com.xiaomo.androidforclaw.agent.skills

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * Skill 文档解析器
 * 支持 AgentSkills.io 格式
 *
 * 格式说明:
 * ---
 * name: skill-name
 * description: 技能描述
 * metadata: { "openclaw": { ... } }
 * ---
 * # Markdown Content
 */
object SkillParser {
    private const val TAG = "SkillParser"
    private val gson = Gson()

    /**
     * 解析 Skill 文档
     *
     * @param content SKILL.md 文件的完整内容
     * @return SkillDocument
     * @throws IllegalArgumentException 如果格式不正确
     */
    fun parse(content: String): SkillDocument {
        try {
            // 1. 分割 frontmatter 和 body
            val (frontmatter, body) = splitFrontmatter(content)

            // 2. 解析 frontmatter 字段
            val name = extractYamlField(frontmatter, "name")
            val description = extractYamlField(frontmatter, "description")
            val metadataJson = extractYamlField(frontmatter, "metadata")

            // 3. 验证必需字段
            if (name.isEmpty()) {
                throw IllegalArgumentException("Missing required field: name")
            }
            if (description.isEmpty()) {
                throw IllegalArgumentException("Missing required field: description")
            }

            // 4. 解析 metadata
            val metadata = parseMetadata(metadataJson)

            return SkillDocument(
                name = name,
                description = description,
                metadata = metadata,
                content = body
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse skill document", e)
            throw IllegalArgumentException("Invalid skill format: ${e.message}", e)
        }
    }

    /**
     * 分割 YAML frontmatter 和 Markdown body
     *
     * 输入:
     * ---
     * name: test
     * ---
     * # Content
     *
     * 输出: ("name: test", "# Content")
     */
    private fun splitFrontmatter(content: String): Pair<String, String> {
        // 使用正则分割 "---" 分隔符
        val parts = content.split(Regex("^---\\s*$", RegexOption.MULTILINE))

        if (parts.size < 3) {
            throw IllegalArgumentException(
                "Invalid format: missing frontmatter delimiters (---)"
            )
        }

        val frontmatter = parts[1].trim()
        val body = parts.drop(2).joinToString("---").trim()

        return Pair(frontmatter, body)
    }

    /**
     * 提取 YAML 字段值
     *
     * 支持格式:
     * 1. 单行: name: value
     * 2. 多行 JSON: metadata: { "key": "value" }
     * 3. 多行 JSON (跨行): metadata:
     *                        {
     *                          "key": "value"
     *                        }
     */
    private fun extractYamlField(yaml: String, field: String): String {
        // 尝试匹配单行格式: field: value
        val singleLineRegex = Regex("$field:\\s*([^\\n{]+)")
        val singleLineMatch = singleLineRegex.find(yaml)
        if (singleLineMatch != null) {
            val value = singleLineMatch.groupValues[1].trim()
            // 如果不是 JSON 起始，返回该值
            if (!value.isEmpty() && !yaml.substring(singleLineMatch.range.last).trimStart().startsWith("{")) {
                return value
            }
        }

        // 尝试匹配多行 JSON: field: { ... } 或 field:\n  { ... }
        // 使用括号计数来正确提取嵌套 JSON
        val fieldPattern = "$field:\\s*"
        val fieldStart = yaml.indexOf(fieldPattern)
        if (fieldStart == -1) {
            return ""
        }

        val jsonStart = yaml.indexOf('{', fieldStart + fieldPattern.length)
        if (jsonStart == -1) {
            return ""
        }

        // 从 { 开始，计数括号直到匹配
        var braceCount = 0
        var jsonEnd = jsonStart
        while (jsonEnd < yaml.length) {
            when (yaml[jsonEnd]) {
                '{' -> braceCount++
                '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        // 找到匹配的右括号
                        val jsonStr = yaml.substring(jsonStart, jsonEnd + 1)
                        // 移除 JSON 中的换行和多余空格，保持紧凑格式
                        return jsonStr.replace(Regex("\\s+"), " ").trim()
                    }
                }
            }
            jsonEnd++
        }

        return ""
    }

    /**
     * 解析 metadata JSON
     *
     * 格式:
     * {
     *   "openclaw": {
     *     "always": true,
     *     "emoji": "📱",
     *     "requires": {
     *       "bins": ["adb"],
     *       "env": ["ANDROID_HOME"],
     *       "config": ["api.key"]
     *     }
     *   }
     * }
     */
    private fun parseMetadata(json: String): SkillMetadata {
        if (json.isEmpty()) {
            return SkillMetadata()
        }

        return try {
            Log.d(TAG, "Parsing metadata JSON (length=${json.length}): $json")
            val jsonObj = gson.fromJson(json, JsonObject::class.java)
            val openclaw = jsonObj.getAsJsonObject("openclaw")

            if (openclaw == null) {
                Log.w(TAG, "metadata.openclaw not found, using defaults")
                return SkillMetadata()
            }

            SkillMetadata(
                always = openclaw.get("always")?.asBoolean ?: false,
                emoji = openclaw.get("emoji")?.asString,
                requires = parseRequires(openclaw)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse metadata JSON (length=${json.length}): $json", e)
            SkillMetadata()
        }
    }

    /**
     * 解析 requires 字段
     */
    private fun parseRequires(openclaw: JsonObject): SkillRequires? {
        val requiresObj = openclaw.getAsJsonObject("requires") ?: return null

        return try {
            SkillRequires(
                bins = jsonArrayToList(requiresObj.getAsJsonArray("bins")),
                env = jsonArrayToList(requiresObj.getAsJsonArray("env")),
                config = jsonArrayToList(requiresObj.getAsJsonArray("config"))
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse requires", e)
            null
        }
    }

    /**
     * 将 JsonArray 转换为 List<String>
     */
    private fun jsonArrayToList(array: JsonArray?): List<String> {
        if (array == null) return emptyList()
        return array.mapNotNull { it.asString }
    }

    /**
     * 验证 Skill 文档格式
     *
     * @return 验证结果，成功返回 null，失败返回错误信息
     */
    fun validate(content: String): String? {
        return try {
            parse(content)
            null  // 验证成功
        } catch (e: Exception) {
            e.message  // 返回错误信息
        }
    }
}
