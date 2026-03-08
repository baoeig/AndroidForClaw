---
name: javascript-executor
description: Execute JavaScript code with QuickJS engine for data processing, text manipulation, and file operations
metadata: { "openclaw": { "always": false, "emoji": "⚡" }, "version": "1.0.0", "category": "computation" }
---

# JavaScript Executor Skill

Execute JavaScript (ES6+) code locally on Android using QuickJS engine. Perfect for data processing, text manipulation, JSON/CSV operations, and file handling.

## 🎯 When to Use

Use `javascript_exec` when you need to:

✅ **Data Processing**:
- Parse and analyze JSON/CSV data
- Transform data structures
- Filter, map, reduce operations
- Statistical calculations

✅ **Text Manipulation**:
- String processing and formatting
- Regular expressions
- Case conversions (camelCase, snake_case, etc.)

✅ **File Operations**:
- Read/write files
- File listing and management
- Batch file processing

✅ **HTTP Requests**:
- Fetch data from APIs
- Simple web scraping
- Data synchronization

## 📚 Available APIs

### Utility Functions (_)

```javascript
// Array operations
_.map([1, 2, 3], x => x * 2)              // [2, 4, 6]
_.filter([1, 2, 3, 4], x => x > 2)        // [3, 4]
_.reduce([1, 2, 3], (a, b) => a + b, 0)   // 6
_.uniq([1, 2, 2, 3, 3])                   // [1, 2, 3]
_.flatten([[1, 2], [3, 4]])               // [1, 2, 3, 4]
_.chunk([1, 2, 3, 4], 2)                  // [[1, 2], [3, 4]]

// Object operations
_.groupBy(users, 'role')                  // Group by key
_.countBy(items, 'category')              // Count by key
_.keyBy(users, 'id')                      // Index by key

// Statistics
_.sum([1, 2, 3, 4])                       // 10
_.mean([1, 2, 3, 4])                      // 2.5
_.max([1, 5, 3])                          // 5
_.min([1, 5, 3])                          // 1

// String operations
_.capitalize('hello')                      // 'Hello'
_.camelCase('hello-world')                 // 'helloWorld'
_.snakeCase('helloWorld')                  // 'hello_world'
_.kebabCase('helloWorld')                  // 'hello-world'
```

### File System (fs)

```javascript
// Read file
const content = fs.readFile('/sdcard/data.json');

// Write file
fs.writeFile('/sdcard/output.txt', 'Hello World');

// Check existence
if (fs.exists('/sdcard/data.json')) {
    // file exists
}

// List directory
const files = fs.listDir('/sdcard/Documents');
// Returns: [{ name, path, isDirectory, size, lastModified }, ...]

// Delete file
fs.delete('/sdcard/temp.txt');
```

### HTTP Client (fetch)

```javascript
// GET request
const response = await fetch('https://api.example.com/data');
const data = await response.json();

// POST request
const response = await fetch('https://api.example.com/users', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({ name: 'John', age: 30 })
});

// Response properties
response.ok           // true if status 200-299
response.status       // HTTP status code
response.text()       // Get response as text
response.json()       // Parse response as JSON
```

### Data Parsing

```javascript
// Parse CSV
const data = parseCSV(`
name,age,city
John,30,NYC
Jane,25,LA
`, { hasHeader: true });
// Returns: [{ name: 'John', age: '30', city: 'NYC' }, ...]

// Safe JSON parsing
const obj = parseJSON('{"key": "value"}', {});  // Default value if parse fails
```

### System Utilities

```javascript
// Log message
System.log('Processing data...');

// Get current timestamp
const now = System.currentTimeMillis();

// Sleep
await System.sleep(1000);  // Sleep 1 second

// Retry with backoff
await retry(async () => {
    return await fetch('https://api.example.com');
}, { maxAttempts: 3, delay: 1000 });

// Timeout wrapper
await timeout(longRunningTask(), 5000);  // 5 second timeout
```

## 💡 Usage Patterns

### Pattern 1: Data Analysis

```javascript
// Read and analyze JSON data
const data = JSON.parse(fs.readFile('/sdcard/sales.json'));

const summary = {
    total: _.sum(data.map(d => d.amount)),
    average: _.mean(data.map(d => d.amount)),
    byCategory: _.groupBy(data, 'category'),
    topItems: _.filter(data, d => d.amount > 1000)
};

return summary;
```

### Pattern 2: CSV Processing

```javascript
// Parse CSV and compute statistics
const csvText = fs.readFile('/sdcard/data.csv');
const rows = parseCSV(csvText, { hasHeader: true });

const stats = {
    totalRows: rows.length,
    avgValue: _.mean(rows.map(r => parseFloat(r.value))),
    categories: _.uniq(rows.map(r => r.category))
};

fs.writeFile('/sdcard/stats.json', JSON.stringify(stats, null, 2));
return stats;
```

### Pattern 3: API Data Fetching

```javascript
// Fetch data from multiple APIs
const users = await fetch('https://api.example.com/users').then(r => r.json());
const posts = await fetch('https://api.example.com/posts').then(r => r.json());

// Merge data
const enrichedUsers = users.map(user => ({
    ...user,
    posts: posts.filter(p => p.userId === user.id)
}));

return enrichedUsers;
```

### Pattern 4: File Batch Processing

```javascript
// Process all JSON files in a directory
const files = fs.listDir('/sdcard/data');
const jsonFiles = files.filter(f => f.name.endsWith('.json'));

const results = jsonFiles.map(file => {
    const content = fs.readFile(file.path);
    const data = parseJSON(content, {});

    return {
        file: file.name,
        recordCount: Array.isArray(data) ? data.length : 0
    };
});

return results;
```

### Pattern 5: Text Processing

```javascript
// Read log file and extract errors
const logs = fs.readFile('/sdcard/app.log');
const lines = logs.split('\n');

const errors = lines.filter(line => line.includes('ERROR'));
const errorSummary = _.countBy(errors, line => {
    const match = line.match(/ERROR: (.+)/);
    return match ? match[1] : 'Unknown';
});

return errorSummary;
```

### Pattern 6: Data Transformation

```javascript
// Transform data format
const rawData = JSON.parse(fs.readFile('/sdcard/raw.json'));

const transformed = rawData.map(item => ({
    id: item.ID,
    fullName: _.capitalize(item.first_name) + ' ' + _.capitalize(item.last_name),
    email: item.email.toLowerCase(),
    createdAt: new Date(item.timestamp).toISOString()
}));

fs.writeFile('/sdcard/transformed.json', JSON.stringify(transformed, null, 2));
return { processedCount: transformed.length };
```

## 🎯 Tool Invocation

```json
{
  "name": "javascript_exec",
  "input": {
    "code": "<your JavaScript code here>",
    "timeout": 30000
  }
}
```

### Parameters

- **code** (string, required): JavaScript code to execute
- **timeout** (number, optional): Execution timeout in milliseconds (default: 30000)

### Return Format

```json
{
  "success": true,
  "content": "<execution result>",
  "metadata": {
    "executionTime": "completed",
    "codeLength": 156
  }
}
```

## ⚠️ Important Notes

### Supported Features

✅ ES6+ syntax (const, let, arrow functions, template strings)
✅ async/await and Promises
✅ Array methods (map, filter, reduce, etc.)
✅ Object methods (Object.keys, Object.values, etc.)
✅ JSON.parse/JSON.stringify
✅ Regular expressions
✅ Math operations

### Limitations

❌ No DOM API (use BrowserForClaw for browser automation)
❌ No Node.js modules (fs, path, os, etc.)
❌ No require() or import statements
❌ Limited setTimeout/setInterval (use System.sleep instead)
❌ No access to Android UI components

### Performance Tips

1. **Keep code concise**: Shorter code executes faster
2. **Avoid infinite loops**: Always use proper loop conditions
3. **Use async/await for I/O**: Don't block the execution thread
4. **Cache results**: Store intermediate results to avoid recomputation
5. **Set appropriate timeout**: Adjust timeout for long-running operations

## 🔍 Debugging

### Enable Logging

```javascript
System.log('Starting data processing...');

const data = JSON.parse(fs.readFile('/sdcard/data.json'));
System.log(`Loaded ${data.length} records`);

// Process data...
System.log('Processing completed');
```

### Error Handling

```javascript
try {
    const result = await fetch('https://api.example.com/data');
    const data = await result.json();
    return data;
} catch (error) {
    System.log('Error: ' + error.message);
    return { error: true, message: error.message };
}
```

### Testing Small Snippets

Start with simple code to verify functionality:

```javascript
// Test 1: Basic math
return 1 + 1;

// Test 2: Array operations
return _.sum([1, 2, 3, 4]);

// Test 3: File read
return fs.exists('/sdcard/test.txt');

// Test 4: HTTP request
const resp = await fetch('https://httpbin.org/get');
return await resp.json();
```

## 📋 Complete Example: Data Analysis Pipeline

```javascript
// 1. Read data from multiple sources
System.log('Loading data sources...');

const salesData = JSON.parse(fs.readFile('/sdcard/sales.json'));
const inventoryData = JSON.parse(fs.readFile('/sdcard/inventory.json'));

// 2. Process and merge data
System.log('Processing data...');

const salesByProduct = _.groupBy(salesData, 'productId');
const inventoryByProduct = _.keyBy(inventoryData, 'productId');

const analysis = Object.keys(salesByProduct).map(productId => {
    const sales = salesByProduct[productId];
    const inventory = inventoryByProduct[productId];

    return {
        productId: productId,
        productName: inventory ? inventory.name : 'Unknown',
        totalSales: _.sum(sales.map(s => s.amount)),
        averageSale: _.mean(sales.map(s => s.amount)),
        transactionCount: sales.length,
        currentStock: inventory ? inventory.quantity : 0,
        needsRestock: inventory && inventory.quantity < 10
    };
});

// 3. Generate summary
const summary = {
    totalProducts: analysis.length,
    totalRevenue: _.sum(analysis.map(a => a.totalSales)),
    productsNeedingRestock: analysis.filter(a => a.needsRestock).length,
    topProducts: _.map(
        _.filter(analysis, a => a.totalSales > 5000)
            .sort((a, b) => b.totalSales - a.totalSales)
            .slice(0, 5),
        a => ({ name: a.productName, sales: a.totalSales })
    )
};

// 4. Save results
System.log('Saving results...');

fs.writeFile('/sdcard/analysis.json', JSON.stringify(analysis, null, 2));
fs.writeFile('/sdcard/summary.json', JSON.stringify(summary, null, 2));

// 5. Return summary
return {
    message: 'Analysis completed successfully',
    summary: summary
};
```

## 🚀 Quick Reference

| Operation | Code |
|-----------|------|
| **Read JSON** | `JSON.parse(fs.readFile(path))` |
| **Write JSON** | `fs.writeFile(path, JSON.stringify(data))` |
| **Parse CSV** | `parseCSV(text, {hasHeader: true})` |
| **HTTP GET** | `await fetch(url).then(r => r.json())` |
| **Array sum** | `_.sum(array)` |
| **Group by** | `_.groupBy(array, 'key')` |
| **Filter** | `_.filter(array, x => x > 10)` |
| **Map** | `_.map(array, x => x * 2)` |
| **Log** | `System.log(message)` |
| **Sleep** | `await System.sleep(1000)` |

## 🎓 When NOT to Use

Avoid `javascript_exec` for:

❌ **Browser Automation**: Use `BrowserForClaw` skills instead
❌ **Android UI Control**: Use `tap`, `swipe`, `screenshot` skills
❌ **Heavy Computation**: Use remote server for intensive tasks
❌ **Real-time Processing**: JavaScript execution has startup overhead

## 🆚 Comparison with Other Tools

| Feature | javascript_exec | BrowserForClaw | Termux |
|---------|----------------|----------------|--------|
| **Data Processing** | ✅ Excellent | ⚠️ Limited | ✅ Excellent |
| **File Operations** | ✅ Full access | ❌ None | ✅ Full access |
| **HTTP Requests** | ✅ Built-in | ✅ Built-in | ✅ via packages |
| **Package Ecosystem** | ❌ None | ❌ None | ✅ npm/pip |
| **Setup Required** | ✅ None | ✅ None | ⚠️ User install |
| **Execution Speed** | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **APK Size Impact** | +10 MB | +0 MB | +0 MB |

---

**JavaScript Executor** - Powered by QuickJS ⚡
