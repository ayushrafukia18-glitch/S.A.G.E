package com.sage.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class MarkdownBlock {
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class BulletItem(val text: String, val level: Int = 0) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String, val level: Int = 0) : MarkdownBlock()
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawText.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // 1. Fenced Code Block
        if (line.trimStart().startsWith("```")) {
            val language = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size && lines[i].trimStart().startsWith("```")) {
                i++ // consume closing ```
            }
            blocks.add(MarkdownBlock.CodeBlock(language = language, code = codeLines.joinToString("\n")))
            continue
        }

        // 2. Headings (# H1, ## H2, ### H3)
        val trimmed = line.trim()
        if (trimmed.startsWith("#")) {
            val hashCount = trimmed.takeWhile { it == '#' }.length
            if (hashCount in 1..4 && trimmed.length > hashCount && trimmed[hashCount] == ' ') {
                val headingText = trimmed.substring(hashCount + 1).trim()
                blocks.add(MarkdownBlock.Heading(level = hashCount, text = headingText))
                i++
                continue
            }
        }

        // 3. Bullet list items (*, -, •)
        val bulletMatch = Regex("""^(\s*)([*•\-])\s+(.*)$""").find(line)
        if (bulletMatch != null) {
            val indent = bulletMatch.groupValues[1].length / 2
            val content = bulletMatch.groupValues[3]
            blocks.add(MarkdownBlock.BulletItem(text = content, level = indent))
            i++
            continue
        }

        // 4. Numbered list items (1., 2., etc.)
        val numberedMatch = Regex("""^(\s*)(\d+[\.\)])\s+(.*)$""").find(line)
        if (numberedMatch != null) {
            val indent = numberedMatch.groupValues[1].length / 2
            val num = numberedMatch.groupValues[2]
            val content = numberedMatch.groupValues[3]
            blocks.add(MarkdownBlock.NumberedItem(number = num, text = content, level = indent))
            i++
            continue
        }

        // 5. Normal text line / paragraph accumulator
        if (trimmed.isEmpty()) {
            i++
            continue
        }

        val paragraphLines = mutableListOf<String>()
        while (i < lines.size) {
            val nextLine = lines[i]
            val nextTrimmed = nextLine.trim()
            if (nextTrimmed.isEmpty() ||
                nextTrimmed.startsWith("```") ||
                nextTrimmed.startsWith("#") ||
                Regex("""^(\s*)([*•\-]|\d+[\.\)])\s+""").containsMatchIn(nextLine)
            ) {
                break
            }
            paragraphLines.add(nextLine)
            i++
        }
        if (paragraphLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString("\n")))
        }
    }

    return blocks
}

fun buildMarkdownAnnotatedString(
    text: String,
    primaryColor: Color,
    codeBackgroundColor: Color,
    codeTextColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val len = text.length

        while (cursor < len) {
            // Inline Code `...`
            if (text[cursor] == '`' && (cursor + 1 < len) && text[cursor + 1] != '`') {
                val endBacktick = text.indexOf('`', cursor + 1)
                if (endBacktick != -1) {
                    val codeContent = text.substring(cursor + 1, endBacktick)
                    val start = length
                    append(codeContent)
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackgroundColor,
                            color = codeTextColor,
                            fontSize = 13.sp
                        ),
                        start,
                        length
                    )
                    cursor = endBacktick + 1
                    continue
                }
            }

            // Bold **...**
            if (cursor + 1 < len && text.substring(cursor, cursor + 2) == "**") {
                val endBold = text.indexOf("**", cursor + 2)
                if (endBold != -1) {
                    val boldContent = text.substring(cursor + 2, endBold)
                    val start = length
                    append(boldContent)
                    addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        start,
                        length
                    )
                    cursor = endBold + 2
                    continue
                }
            }

            // Bold __...__
            if (cursor + 1 < len && text.substring(cursor, cursor + 2) == "__") {
                val endBold = text.indexOf("__", cursor + 2)
                if (endBold != -1) {
                    val boldContent = text.substring(cursor + 2, endBold)
                    val start = length
                    append(boldContent)
                    addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        start,
                        length
                    )
                    cursor = endBold + 2
                    continue
                }
            }

            // Italic *...*
            if (text[cursor] == '*' && (cursor + 1 < len) && text[cursor + 1] != '*') {
                val endItalic = text.indexOf('*', cursor + 1)
                if (endItalic != -1 && endItalic > cursor + 1) {
                    val italicContent = text.substring(cursor + 1, endItalic)
                    val start = length
                    append(italicContent)
                    addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic),
                        start,
                        length
                    )
                    cursor = endItalic + 1
                    continue
                }
            }

            // Strikethrough ~~...~~
            if (cursor + 1 < len && text.substring(cursor, cursor + 2) == "~~") {
                val endStrike = text.indexOf("~~", cursor + 2)
                if (endStrike != -1) {
                    val strikeContent = text.substring(cursor + 2, endStrike)
                    val start = length
                    append(strikeContent)
                    addStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough),
                        start,
                        length
                    )
                    cursor = endStrike + 2
                    continue
                }
            }

            // Normal character
            append(text[cursor])
            cursor++
        }
    }
}

@Composable
fun MarkdownContent(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    isUser: Boolean = false
) {
    val blocks = parseMarkdownBlocks(text)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val codeBgColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)
    }

    val codeTextColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(6.dp))
            }

            when (block) {
                is MarkdownBlock.Heading -> {
                    val headingStyle = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    val headingText = buildMarkdownAnnotatedString(
                        text = block.text,
                        primaryColor = textColor,
                        codeBackgroundColor = codeBgColor,
                        codeTextColor = codeTextColor
                    )
                    Text(
                        text = headingText,
                        style = headingStyle,
                        color = textColor
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    val annotated = buildMarkdownAnnotatedString(
                        text = block.text,
                        primaryColor = textColor,
                        codeBackgroundColor = codeBgColor,
                        codeTextColor = codeTextColor
                    )
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        ),
                        color = textColor
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    val annotated = buildMarkdownAnnotatedString(
                        text = block.text,
                        primaryColor = textColor,
                        codeBackgroundColor = codeBgColor,
                        codeTextColor = codeTextColor
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.level * 12).dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = textColor,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                lineHeight = 21.sp
                            ),
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    val annotated = buildMarkdownAnnotatedString(
                        text = block.text,
                        primaryColor = textColor,
                        codeBackgroundColor = codeBgColor,
                        codeTextColor = codeTextColor
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.level * 12).dp)
                    ) {
                        Text(
                            text = block.number,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = textColor,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                lineHeight = 21.sp
                            ),
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isUser) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        tonalElevation = 1.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = block.language.ifBlank { "code" },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(block.code))
                                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy code block",
                                        tint = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = block.code,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    ),
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
