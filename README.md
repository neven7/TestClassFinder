# TestClassFinder
TestClassFinder,support JUnit &amp; TestNG;  测试类探测器，支持JUnix和TestNG，能找出满足正则的所有测试类.


将工程打包成jar包，src/test/java中使用该jar包

e.g.
// 正则匹配
String[] filterPatterns = { "com.weibo.qa.testclassfinder.test.*Test" };
TestClassFinder tcn = new TestClassFinder(filterPatterns);

System.out.println(tcn.find());
System.out.println(tcn.classNameList(tcn.find()));


输出：
[class com.weibo.qa.testclassfinder.test.JUnitTest]
[JUnitTest]
