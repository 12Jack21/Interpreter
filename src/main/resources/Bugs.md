### 词法分析
- [ ] 函数的功能，float的完整词法功能，可视化，测试用例的整合，多个声明，声明时初始化
- if 加缩进，减法的优先级问题
- 多错误处理

- p--，超前搜索后未回退 
- 换行时 列号未重新初始化
- Extend
    1. 科学计数法 `real`
    2. 十六进制数 `hexadecimal`
### 语法分析

1. 利用词法分析器扫描时，只有在终结符匹配时才继续下一次扫描，
而不是每次循环都扫描一次
2. 一开始用每个 char来表示一个终结符或非终结符，后来不够用了，
改成了 String，也不再用 逗号分割产生式，而是用空格来取代，
顺便还消除了单独考虑 逗号的麻烦

### TODO list
- [x] '|' 和 '&'的词法判别错误处理
- [x] 字符串的实现

### 语义分析
- Basic
	1. 变量，变量表
	2. 数组，数组表
	3. 拿到 identifier的 value和 type
	4. 输入输出语句
	5. 逻辑表达式的短路求值，直接return
	6. 数组的赋初始值，与未赋值时的使用问题
	7. while的break和continue问题
- Extend
	1. 字符串加法
	2. 二维数组
	3. 函数调用栈
	4. 对于循环的语义消息可以通过**树**的结构展现(数量太大)
	5. char s[2][3] = {"qwe","q12"};的情况
	6. a++，--a, a = b = 2
	7. 十六进制(int)，科学计数法(real)
    8. 位运算， | 、&、~, 特别地还有 ~0,~11
    9. += 运算，把 a=3加入表达式也是类似
### 错误列表
- Basic
	1. 多重声明 
	2. 没有声明
	3. 使用时没有初始化
	3. 类型转换（先进行自动转换）
	4. if 和 while中的条件语句 出现类型问题，不为int
	5. 除零错误
	6. 数组索引越界
	7. 数组下标不是整数，不是正数
	8. 简单变量初始化为数组变量
	9. 数组变量和简单变量名相同
	10. Overflow Exception
- Extend
	1. 死循环 （条件恒为 1 或 0）
	2. 常量左值不可修改
	3. 函数未声明，参数列表不匹配

### Passed Test
1. implicitly cast, operation test(without overflow exception throw) and CHAR & STRING (without explicitly type transfer)in type.c
2. matrix operation but some code error in the original file
3. bisearch and stack have passes before
4. pass sum (don't know the meaning of id totalreal)
5. limitation runs out of memory
6. pass insert sort
7. pass gcd but it cannot calculate when a = n* b while n is a integer
8. pass 01 bag problem, has many error in original code
### Solved problem
- 用深拷贝解决了函数递归调用时的ASTNode 共享问题，但在逻辑表达式中依然保留 flushfindtag()

### JavaCC 
- [javacc LOOKAHEAD analysis](http://www.voidcn.com/article/p-dlmrztul-xs.html)
- JJtree tools to generate AST