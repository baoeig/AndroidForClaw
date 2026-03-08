# JavaScript Executor - 使用示例

QuickJS JavaScript 执行器的完整使用指南和示例。

## 📋 目录

1. [基础用法](#基础用法)
2. [数据处理](#数据处理)
3. [文件操作](#文件操作)
4. [HTTP 请求](#http-请求)
5. [文本处理](#文本处理)
6. [实际应用场景](#实际应用场景)

---

## 基础用法

### 示例 1: 简单计算

```json
{
  "name": "javascript_exec",
  "input": {
    "code": "return 1 + 2 + 3;"
  }
}
```

**结果**: `6`

### 示例 2: 数组操作

```json
{
  "name": "javascript_exec",
  "input": {
    "code": "const numbers = [1, 2, 3, 4, 5];\nconst doubled = _.map(numbers, x => x * 2);\nreturn _.sum(doubled);"
  }
}
```

**结果**: `30`

### 示例 3: 对象操作

```json
{
  "name": "javascript_exec",
  "input": {
    "code": "const users = [\n  { name: 'John', age: 30 },\n  { name: 'Jane', age: 25 },\n  { name: 'Bob', age: 30 }\n];\nconst grouped = _.groupBy(users, 'age');\nreturn JSON.stringify(grouped, null, 2);"
  }
}
```

**结果**:
```json
{
  "25": [{"name": "Jane", "age": 25}],
  "30": [{"name": "John", "age": 30}, {"name": "Bob", "age": 30}]
}
```

---

## 数据处理

### 示例 4: JSON 数据分析

```javascript
// 读取并分析 JSON 数据
const data = JSON.parse(fs.readFile('/sdcard/sales.json'));

const analysis = {
    totalRecords: data.length,
    totalRevenue: _.sum(data.map(d => d.amount)),
    averageRevenue: _.mean(data.map(d => d.amount)),
    byCategory: _.countBy(data, 'category'),
    topItems: _.filter(data, d => d.amount > 1000)
        .sort((a, b) => b.amount - a.amount)
        .slice(0, 5)
};

return JSON.stringify(analysis, null, 2);
```

### 示例 5: CSV 数据处理

```javascript
// 读取 CSV 并计算统计信息
const csvText = fs.readFile('/sdcard/data.csv');
const rows = parseCSV(csvText, { hasHeader: true });

// 计算每个类别的平均值
const byCategory = _.groupBy(rows, 'category');
const categoryStats = Object.keys(byCategory).map(cat => ({
    category: cat,
    count: byCategory[cat].length,
    avgValue: _.mean(byCategory[cat].map(r => parseFloat(r.value)))
}));

return JSON.stringify(categoryStats, null, 2);
```

### 示例 6: 数据清洗

```javascript
// 清洗和标准化数据
const rawData = JSON.parse(fs.readFile('/sdcard/raw_data.json'));

const cleanedData = rawData
    .filter(item => item.id && item.name)  // 移除无效记录
    .map(item => ({
        id: item.id,
        name: _.capitalize(item.name.trim()),
        email: item.email ? item.email.toLowerCase() : null,
        amount: parseFloat(item.amount) || 0,
        category: _.snakeCase(item.category || 'unknown')
    }));

// 保存清洗后的数据
fs.writeFile('/sdcard/cleaned_data.json', JSON.stringify(cleanedData, null, 2));

return { processedCount: cleanedData.length };
```

---

## 文件操作

### 示例 7: 批量文件重命名

```javascript
// 读取目录中的所有文件
const files = fs.listDir('/sdcard/photos');

// 重命名所有 .jpg 文件
const renamedCount = files
    .filter(f => f.name.endsWith('.jpg') && !f.isDirectory)
    .map((file, index) => {
        const newName = `photo_${index + 1}.jpg`;
        const newPath = file.path.replace(file.name, newName);

        // 这里只是生成新文件名,实际重命名需要其他工具
        return { old: file.name, new: newName };
    })
    .length;

return { renamedCount, files: renamedCount };
```

### 示例 8: 文件内容合并

```javascript
// 合并多个文本文件
const files = [
    '/sdcard/log1.txt',
    '/sdcard/log2.txt',
    '/sdcard/log3.txt'
];

const combinedContent = files
    .filter(f => fs.exists(f))
    .map(f => fs.readFile(f))
    .join('\n\n--- Next File ---\n\n');

fs.writeFile('/sdcard/combined_logs.txt', combinedContent);

return { message: 'Files merged successfully', totalSize: combinedContent.length };
```

### 示例 9: 目录结构分析

```javascript
// 分析目录结构
const files = fs.listDir('/sdcard/Documents');

const stats = {
    totalFiles: files.filter(f => !f.isDirectory).length,
    totalDirs: files.filter(f => f.isDirectory).length,
    totalSize: _.sum(files.map(f => f.size)),
    fileTypes: _.countBy(
        files.filter(f => !f.isDirectory),
        f => f.name.split('.').pop() || 'no-extension'
    ),
    largestFile: files.reduce((max, f) =>
        f.size > max.size ? f : max,
        { name: 'none', size: 0 }
    )
};

return JSON.stringify(stats, null, 2);
```

---

## HTTP 请求

### 示例 10: API 数据获取

```javascript
// 获取 API 数据
const response = await fetch('https://api.example.com/users');
const users = await response.json();

// 处理数据
const activeUsers = users.filter(u => u.status === 'active');
const usersByRole = _.groupBy(activeUsers, 'role');

return JSON.stringify({
    total: users.length,
    active: activeUsers.length,
    byRole: Object.keys(usersByRole).map(role => ({
        role,
        count: usersByRole[role].length
    }))
}, null, 2);
```

### 示例 11: 批量 API 请求

```javascript
// 获取多个用户的详细信息
const userIds = [1, 2, 3, 4, 5];

const users = [];
for (const id of userIds) {
    try {
        const response = await fetch(`https://api.example.com/users/${id}`);
        const user = await response.json();
        users.push(user);

        // 避免请求过快
        await System.sleep(100);
    } catch (error) {
        System.log(`Failed to fetch user ${id}: ${error.message}`);
    }
}

return JSON.stringify({ fetchedUsers: users.length, users }, null, 2);
```

### 示例 12: POST 请求提交数据

```javascript
// 提交数据到 API
const payload = {
    name: 'John Doe',
    email: 'john@example.com',
    age: 30
};

const response = await fetch('https://api.example.com/users', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
});

const result = await response.json();

return JSON.stringify({
    success: response.ok,
    status: response.status,
    data: result
}, null, 2);
```

---

## 文本处理

### 示例 13: 日志文件分析

```javascript
// 分析日志文件中的错误
const logContent = fs.readFile('/sdcard/app.log');
const lines = logContent.split('\n');

// 提取错误行
const errors = lines.filter(line => line.includes('ERROR'));

// 统计错误类型
const errorTypes = _.countBy(errors, line => {
    const match = line.match(/ERROR: (.+?) -/);
    return match ? match[1] : 'Unknown';
});

// 获取最近的 5 个错误
const recentErrors = errors.slice(-5);

return JSON.stringify({
    totalLines: lines.length,
    errorCount: errors.length,
    errorTypes,
    recentErrors
}, null, 2);
```

### 示例 14: 文本格式转换

```javascript
// 将驼峰命名转换为蛇形命名
const code = fs.readFile('/sdcard/code.txt');

const converted = code
    .split('\n')
    .map(line => {
        // 替换驼峰命名为蛇形
        return line.replace(/([a-z])([A-Z])/g, '$1_$2').toLowerCase();
    })
    .join('\n');

fs.writeFile('/sdcard/code_snake_case.txt', converted);

return { message: 'Conversion completed', lines: converted.split('\n').length };
```

### 示例 15: 文本提取和清理

```javascript
// 从 HTML 标签中提取纯文本
const htmlContent = fs.readFile('/sdcard/page.html');

// 简单的标签移除 (不完美但足够基本场景)
const plainText = htmlContent
    .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')  // 移除 script
    .replace(/<style[^>]*>[\s\S]*?<\/style>/gi, '')    // 移除 style
    .replace(/<[^>]+>/g, '')                            // 移除所有标签
    .replace(/\s+/g, ' ')                               // 压缩空白
    .trim();

// 提取关键词 (最常见的单词)
const words = plainText.toLowerCase()
    .split(/\s+/)
    .filter(w => w.length > 3);

const wordCount = _.countBy(words);
const topWords = Object.entries(wordCount)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10)
    .map(([word, count]) => ({ word, count }));

return JSON.stringify({
    textLength: plainText.length,
    wordCount: words.length,
    topWords
}, null, 2);
```

---

## 实际应用场景

### 场景 1: 数据报告生成

```javascript
// 生成每日销售报告
System.log('Generating daily sales report...');

// 1. 读取数据
const salesData = JSON.parse(fs.readFile('/sdcard/sales/today.json'));
const inventoryData = JSON.parse(fs.readFile('/sdcard/inventory/current.json'));

// 2. 分析销售数据
const salesByProduct = _.groupBy(salesData, 'productId');
const inventoryByProduct = _.keyBy(inventoryData, 'id');

// 3. 生成报告
const report = {
    date: new Date().toISOString().split('T')[0],
    summary: {
        totalTransactions: salesData.length,
        totalRevenue: _.sum(salesData.map(s => s.amount)),
        averageTransaction: _.mean(salesData.map(s => s.amount))
    },
    products: Object.keys(salesByProduct).map(pid => {
        const sales = salesByProduct[pid];
        const inventory = inventoryByProduct[pid];

        return {
            productId: pid,
            productName: inventory ? inventory.name : 'Unknown',
            unitsSold: sales.length,
            revenue: _.sum(sales.map(s => s.amount)),
            currentStock: inventory ? inventory.quantity : 0,
            needsRestock: inventory && inventory.quantity < 10
        };
    }).sort((a, b) => b.revenue - a.revenue),
    alerts: []
};

// 4. 添加告警
report.products.forEach(p => {
    if (p.needsRestock) {
        report.alerts.push(`Low stock alert: ${p.productName} (${p.currentStock} units)`);
    }
});

// 5. 保存报告
const reportPath = `/sdcard/reports/daily_${report.date}.json`;
fs.writeFile(reportPath, JSON.stringify(report, null, 2));

System.log(`Report generated: ${reportPath}`);

return {
    message: 'Report generated successfully',
    reportPath,
    summary: report.summary,
    alerts: report.alerts
};
```

### 场景 2: 配置文件验证

```javascript
// 验证配置文件格式和内容
System.log('Validating configuration files...');

const configFiles = [
    '/sdcard/config/app.json',
    '/sdcard/config/database.json',
    '/sdcard/config/api.json'
];

const errors = [];
const warnings = [];

configFiles.forEach(filePath => {
    if (!fs.exists(filePath)) {
        errors.push(`Missing config file: ${filePath}`);
        return;
    }

    const content = fs.readFile(filePath);
    const config = parseJSON(content);

    if (!config) {
        errors.push(`Invalid JSON in: ${filePath}`);
        return;
    }

    // 验证必需字段
    const fileName = filePath.split('/').pop().replace('.json', '');

    if (fileName === 'app' && !config.appName) {
        errors.push('app.json missing required field: appName');
    }

    if (fileName === 'database' && !config.host) {
        errors.push('database.json missing required field: host');
    }

    // 检查废弃字段
    if (config.deprecated_field) {
        warnings.push(`${fileName}.json uses deprecated field: deprecated_field`);
    }
});

const result = {
    valid: errors.length === 0,
    errors,
    warnings
};

System.log(`Validation complete: ${result.valid ? 'PASSED' : 'FAILED'}`);

return JSON.stringify(result, null, 2);
```

### 场景 3: 数据迁移脚本

```javascript
// 数据格式迁移 (v1 -> v2)
System.log('Starting data migration...');

// 1. 读取旧格式数据
const oldData = JSON.parse(fs.readFile('/sdcard/data_v1.json'));

// 2. 转换为新格式
const newData = oldData.map(item => ({
    // 新字段
    id: item.old_id,
    fullName: `${item.first_name} ${item.last_name}`,
    contact: {
        email: item.email,
        phone: item.phone_number
    },
    metadata: {
        createdAt: new Date(item.timestamp).toISOString(),
        version: 2,
        migratedFrom: 1
    },
    // 保留的字段
    status: item.status,
    // 移除的字段不再包含: old_field_1, old_field_2
}));

// 3. 验证迁移
const validRecords = newData.filter(item =>
    item.id && item.fullName && item.contact.email
);

if (validRecords.length !== newData.length) {
    return {
        success: false,
        error: `Validation failed: ${newData.length - validRecords.length} invalid records`
    };
}

// 4. 保存新数据
fs.writeFile('/sdcard/data_v2.json', JSON.stringify(validRecords, null, 2));

// 5. 创建备份
fs.writeFile('/sdcard/data_v1_backup.json', JSON.stringify(oldData, null, 2));

System.log('Migration completed successfully');

return {
    success: true,
    migratedRecords: validRecords.length,
    message: 'Data migrated from v1 to v2'
};
```

### 场景 4: API 数据同步

```javascript
// 从远程 API 同步数据到本地
System.log('Starting data sync...');

const syncResults = {
    synced: 0,
    failed: 0,
    errors: []
};

try {
    // 1. 获取远程数据
    const response = await fetch('https://api.example.com/data');
    if (!response.ok) {
        throw new Error(`API request failed: ${response.status}`);
    }

    const remoteData = await response.json();
    System.log(`Fetched ${remoteData.length} records from API`);

    // 2. 读取本地数据
    const localDataPath = '/sdcard/sync/local_data.json';
    const localData = fs.exists(localDataPath)
        ? JSON.parse(fs.readFile(localDataPath))
        : [];

    const localDataById = _.keyBy(localData, 'id');

    // 3. 合并数据 (远程优先)
    const mergedData = remoteData.map(remoteItem => {
        const localItem = localDataById[remoteItem.id];

        // 远程数据更新或新增
        if (!localItem || remoteItem.updatedAt > localItem.updatedAt) {
            syncResults.synced++;
            return remoteItem;
        }

        // 保留本地数据
        return localItem;
    });

    // 4. 添加只在本地存在的数据
    localData.forEach(localItem => {
        if (!remoteData.find(r => r.id === localItem.id)) {
            mergedData.push(localItem);
        }
    });

    // 5. 保存合并后的数据
    fs.writeFile(localDataPath, JSON.stringify(mergedData, null, 2));

    // 6. 记录同步时间
    fs.writeFile('/sdcard/sync/last_sync.txt', new Date().toISOString());

    System.log('Sync completed successfully');

} catch (error) {
    syncResults.failed++;
    syncResults.errors.push(error.message);
    System.log(`Sync failed: ${error.message}`);
}

return JSON.stringify(syncResults, null, 2);
```

---

## 性能优化建议

### 1. 避免重复计算

```javascript
// ❌ 不好 - 重复读取文件
const data1 = JSON.parse(fs.readFile('/sdcard/data.json'));
const data2 = JSON.parse(fs.readFile('/sdcard/data.json'));

// ✅ 好 - 读取一次,重复使用
const data = JSON.parse(fs.readFile('/sdcard/data.json'));
const processed1 = processData(data);
const processed2 = transformData(data);
```

### 2. 批量处理

```javascript
// ❌ 不好 - 逐个处理
items.forEach(item => {
    const result = expensiveOperation(item);
    fs.writeFile(`/sdcard/output/${item.id}.json`, JSON.stringify(result));
});

// ✅ 好 - 批量处理后一次写入
const results = items.map(item => ({
    id: item.id,
    result: expensiveOperation(item)
}));
fs.writeFile('/sdcard/output/all_results.json', JSON.stringify(results));
```

### 3. 使用适当的数据结构

```javascript
// ❌ 不好 - 使用数组查找
const user = users.find(u => u.id === targetId);

// ✅ 好 - 使用对象索引
const usersById = _.keyBy(users, 'id');
const user = usersById[targetId];
```

---

## 错误处理最佳实践

```javascript
// 完整的错误处理示例
async function robustDataProcessing() {
    const results = {
        success: false,
        data: null,
        errors: []
    };

    try {
        // 1. 验证文件存在
        if (!fs.exists('/sdcard/input.json')) {
            throw new Error('Input file not found');
        }

        // 2. 读取和解析数据
        const content = fs.readFile('/sdcard/input.json');
        const data = parseJSON(content);

        if (!data) {
            throw new Error('Invalid JSON format');
        }

        // 3. 处理数据 (带重试)
        const processed = await retry(async () => {
            return await expensiveOperation(data);
        }, { maxAttempts: 3, delay: 1000 });

        // 4. 保存结果
        fs.writeFile('/sdcard/output.json', JSON.stringify(processed, null, 2));

        results.success = true;
        results.data = processed;

    } catch (error) {
        results.errors.push(error.message);
        System.log(`Error: ${error.message}`);
    }

    return JSON.stringify(results, null, 2);
}

return await robustDataProcessing();
```

---

**JavaScript Executor** - Powered by QuickJS ⚡
