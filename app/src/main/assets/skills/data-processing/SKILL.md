---
name: data-processing
description: Data processing and analysis using JavaScript
metadata:
  {
    "openclaw": {
      "always": false,
      "emoji": "📊"
    }
  }
---

# Data Processing Skill

Use JavaScript for data transformation, analysis, and calculations.

## When to Use

- Process JSON/CSV data
- Calculate statistics (sum, average, max, min)
- Transform arrays and objects
- Parse and format strings
- Complex math calculations
- Data filtering and grouping

## Available Tool

### javascript

Execute JavaScript code with ES6+ support.

**Syntax:**
```json
{
  "tool": "javascript",
  "args": {
    "code": "const arr = [1,2,3]; arr.map(x => x * 2)"
  }
}
```

The last expression value is automatically returned.

## Examples

### Example 1: Array Statistics

Calculate sum, average, max, min:

```javascript
const numbers = [15, 23, 8, 42, 16, 4, 31];

const sum = numbers.reduce((a, b) => a + b, 0);
const avg = sum / numbers.length;
const max = Math.max(...numbers);
const min = Math.min(...numbers);

JSON.stringify({ sum, avg, max, min });
```

Result: `{"sum":139,"avg":19.857142857142858,"max":42,"min":4}`

### Example 2: Filter and Transform

Filter even numbers and double them:

```javascript
const data = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

const result = data
  .filter(x => x % 2 === 0)
  .map(x => x * 2);

JSON.stringify(result);
```

Result: `[4,8,12,16,20]`

### Example 3: Group Data

Group items by category:

```javascript
const items = [
  { name: "apple", category: "fruit", price: 1.2 },
  { name: "banana", category: "fruit", price: 0.8 },
  { name: "carrot", category: "vegetable", price: 0.5 },
  { name: "broccoli", category: "vegetable", price: 1.5 }
];

const grouped = items.reduce((acc, item) => {
  const cat = item.category;
  if (!acc[cat]) acc[cat] = [];
  acc[cat].push(item);
  return acc;
}, {});

JSON.stringify(grouped, null, 2);
```

### Example 4: String Processing

Parse and format text:

```javascript
const text = "Hello, World! How are you?";

const result = {
  length: text.length,
  words: text.split(/\s+/).length,
  uppercase: text.toUpperCase(),
  reversed: text.split('').reverse().join(''),
  firstWord: text.split(/\s+/)[0]
};

JSON.stringify(result, null, 2);
```

### Example 5: Calculate Percentage

```javascript
const total = 1500;
const current = 450;

const percentage = ((current / total) * 100).toFixed(2);
const remaining = total - current;

JSON.stringify({
  percentage: percentage + "%",
  current,
  remaining,
  total
});
```

## Best Practices

1. **Use JSON.stringify()** for complex results (objects/arrays)
2. **Last expression is returned** - no need for explicit return
3. **ES6+ features available**: arrow functions, const/let, template strings, destructuring
4. **Built-in methods**: Array (map, filter, reduce), String, Math, Object, JSON

## Common Patterns

### Sum an array
```javascript
arr.reduce((a, b) => a + b, 0)
```

### Find unique values
```javascript
[...new Set(arr)]
```

### Count occurrences
```javascript
arr.reduce((acc, val) => {
  acc[val] = (acc[val] || 0) + 1;
  return acc;
}, {})
```

### Flatten nested array
```javascript
arr.flat(Infinity)
```

### Sort array
```javascript
arr.sort((a, b) => a - b)  // ascending
arr.sort((a, b) => b - a)  // descending
```

## Limitations

- **No async/await** - Use synchronous code only
- **No Node.js APIs** - Pure JavaScript only (no fs, http, etc.)
- **No DOM** - Browser APIs not available
- **No external libraries** - Built-in JavaScript only

## Tips

- Keep code simple and focused
- Use clear variable names
- Test with small data first
- Format output with JSON.stringify() for readability
