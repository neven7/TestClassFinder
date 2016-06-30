package com.weibo.qa.testclassfinder;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 根据包名，找出所有的类名(非全限类名， mvn不支持，只支持简单的类名)
 * 
 * @author hugang
 *
 */
public class TestClassFinder {

	private static final boolean DEFAULT_INCLUDE_JARS = false;
	private static final SuiteType[] DEFAULT_SUITE_TYPES = new SuiteType[] { SuiteType.TEST_CLASSES };
	private static final Class<?>[] DEFAULT_BASE_TYPES = new Class<?>[] { Object.class };
	private static final Class<?>[] DEFAULT_EXCLUDED_BASES_TYPES = new Class<?>[0];
	private static final String[] DEFAULT_CLASSNAME_FILTERS = new String[0];

	private static final int CLASS_SUFFIX_LENGTH = ".class".length();
	private static final String FALLBACK_CLASSPATH_PROPERTY = "java.class.path";

	private final String[] filterPatterns;
	private final ClassTester tester;
	private final String classpathProperty = FALLBACK_CLASSPATH_PROPERTY;

	/**
	 * The <code>ClassnameFilters</code> annotation specifies a set of regex
	 * expressions for all test classes (ie. their qualified names) to include
	 * in the test run. When the annotation is missing, all test classes in all
	 * packages will be run.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ClassnameFilters {
		public String[] value();
	}

	/**
	 * The <code>IncludeJars</code> annotation specifies if Jars should be
	 * searched in or not. If the annotation is missing Jars are not being
	 * searched.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface IncludeJars {
		public boolean value();
	}

	/**
	 * The <code>SuiteTypes</code> annotation specifies which types of tests
	 * will be included in the test run. You can choose one or more from
	 * TEST_CLASSES, RUN_WITH_CLASSES, JUNIT38_TEST_CLASSES. If none is
	 * specified only JUnit4-style TEST_CLASSES will be run.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface SuiteTypes {
		public SuiteType[] value();
	}

	/**
	 * The <code>BaseTypeFilter</code> annotation filters all test classes to be
	 * run by one or several given base types, i.e. only those classes will be
	 * run which extend one of the base types. Default is
	 * <code>Object.class</code>.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface BaseTypeFilter {
		public Class<?>[] value();
	}

	/**
	 * The <code>ExcludeBaseTypeFilter</code> annotation filters all test
	 * classes to be run by one or several given base types, i.e. only those
	 * classes will be run which <em>do not extend</em> any of the base types.
	 * Default is <code>Object.class</code>.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ExcludeBaseTypeFilter {
		public Class<?>[] value();
	}

	/**
	 * The <code>ClasspathProperty</code> specifies the System property name
	 * used to retrieve the java classpath which is searched for Test classes
	 * and suites. Default is "java.class.path".
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ClasspathProperty {
		String value();
	}

	/**
	 * The <code>BeforeSuite</code> marks a method that will be run before the
	 * suite is run.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface BeforeSuite {
	}

	private static String[] getClassnameFilters(Class<?> suiteClass) {
		ClassnameFilters filtersAnnotation = suiteClass
				.getAnnotation(ClassnameFilters.class);
		if (filtersAnnotation == null) {
			return DEFAULT_CLASSNAME_FILTERS;
		}
		return filtersAnnotation.value();
	}

	// 构造函数，初始化正则匹配表达式
	public TestClassFinder(String[] filterPatterns) {
		this.filterPatterns = filterPatterns;

		// boolean searchInJars, String[] filterPatterns, SuiteType[]
		// suiteTypes, Class<?>[] baseTypes,Class<?>[] excludedBaseTypes
		// this.tester = new
		// ClasspathSuiteTester(getSearchInJars(this.getClass()),
		// getClassnameFilters(this.getClass()), getSuiteTypes(this.getClass()),
		// getBaseTypes(this.getClass()),
		// getExcludedBaseTypes(this.getClass()));

		this.tester = new ClasspathSuiteTester(
				getSearchInJars(this.getClass()), filterPatterns,
				getSuiteTypes(this.getClass()), getBaseTypes(this.getClass()),
				getExcludedBaseTypes(this.getClass()));

	}

	private static boolean getSearchInJars(Class<?> suiteClass) {
		IncludeJars includeJarsAnnotation = suiteClass
				.getAnnotation(IncludeJars.class);
		if (includeJarsAnnotation == null) {
			return DEFAULT_INCLUDE_JARS;
		}
		return includeJarsAnnotation.value();
	}

	private static SuiteType[] getSuiteTypes(Class<?> suiteClass) {
		SuiteTypes suiteTypesAnnotation = suiteClass
				.getAnnotation(SuiteTypes.class);
		if (suiteTypesAnnotation == null) {
			return DEFAULT_SUITE_TYPES;
		}
		return suiteTypesAnnotation.value();
	}

	private static Class<?>[] getBaseTypes(Class<?> suiteClass) {
		BaseTypeFilter baseTypeAnnotation = suiteClass
				.getAnnotation(BaseTypeFilter.class);
		if (baseTypeAnnotation == null) {
			return DEFAULT_BASE_TYPES;
		}
		return baseTypeAnnotation.value();
	}

	private static Class<?>[] getExcludedBaseTypes(Class<?> suiteClass) {
		ExcludeBaseTypeFilter excludeBaseTypeAnnotation = suiteClass
				.getAnnotation(ExcludeBaseTypeFilter.class);
		if (excludeBaseTypeAnnotation == null) {
			return DEFAULT_EXCLUDED_BASES_TYPES;
		}
		return excludeBaseTypeAnnotation.value();
	}

	// step 1
	public List<Class<?>> find() {
		return findClassesInClasspath(getClasspath());
	}

	private String getClasspath() {
		String classPath = System.getProperty(getClasspathProperty());
		if (classPath == null)
			classPath = System.getProperty(FALLBACK_CLASSPATH_PROPERTY);
		return classPath;
	}

	// step 2
	private List<Class<?>> findClassesInClasspath(String classPath) {
		return findClassesInRoots(splitClassPath(classPath));
	}

	// step 3
	// hg, 从所有的class中进行匹配
	private List<Class<?>> findClassesInRoots(List<String> roots) {
		List<Class<?>> classes = new ArrayList<Class<?>>(100);
		for (String root : roots) {
			gatherClassesInRoot(new File(root), classes);
		}
		return classes;
	}

	private void gatherClassesInRoot(File classRoot, List<Class<?>> classes) {
		Iterable<String> relativeFilenames = new NullIterator<String>();
		if (tester.searchInJars() && isJarFile(classRoot)) {
			try {
				relativeFilenames = new JarFilenameIterator(classRoot);
			} catch (IOException e) {
				// Don't iterate unavailable ja files
				e.printStackTrace();
			}
		} else if (classRoot.isDirectory()) {
			relativeFilenames = new RecursiveFilenameIterator(classRoot);
		}
		gatherClasses(classes, relativeFilenames);
	}

	public String getClasspathProperty() {
		return classpathProperty;
	}

	private boolean isJarFile(File classRoot) {
		return classRoot.getName().endsWith(".jar")
				|| classRoot.getName().endsWith(".JAR");
	}

	private List<String> splitClassPath(String classPath) {
		final String separator = System.getProperty("path.separator");
		return Arrays.asList(classPath.split(separator));
	}

	private boolean isInnerClass(String className) {
		return className.contains("$");
	}

	private boolean isClassFile(String classFileName) {
		return classFileName.endsWith(".class");
	}

	private String classNameFromFile(String classFileName) {
		// convert /a/b.class to a.b
		String s = replaceFileSeparators(cutOffExtension(classFileName));
		if (s.startsWith("."))
			return s.substring(1);
		return s;
	}

	private String replaceFileSeparators(String s) {
		String result = s.replace(File.separatorChar, '.');
		if (File.separatorChar != '/') {
			// In Jar-Files it's always '/'
			result = result.replace('/', '.');
		}
		return result;
	}

	private String cutOffExtension(String classFileName) {
		return classFileName.substring(0, classFileName.length()
				- CLASS_SUFFIX_LENGTH);
	}

	private void gatherClasses(List<Class<?>> classes,
			Iterable<String> filenamesIterator) {
		for (String fileName : filenamesIterator) {
			if (!isClassFile(fileName)) {
				continue;
			}
			// 类文件 转换成 全限定类名
			// com/weibo/batchexecution/BatchComments.class
			// com.weibo.batchexecution.BatchComments
			String className = classNameFromFile(fileName);
			// acceptClassName() 正则匹配类名

			// tester为ClasspathSuiteTester实例
			if (!tester.acceptClassName(className)) {
				continue;
			}
			if (!tester.acceptInnerClass() && isInnerClass(className)) {
				continue;
			}
			try {
				Class<?> clazz = Class.forName(className, false, getClass()
						.getClassLoader());
				if (clazz == null || clazz.isLocalClass()
						|| clazz.isAnonymousClass()) {
					continue;
				}
				if (tester.acceptClass(clazz)) {
					classes.add(clazz);
				}
			} catch (ClassNotFoundException cnfe) {
				// ignore not instantiable classes
			} catch (NoClassDefFoundError ncdfe) {
				// ignore not instantiable classes
			} catch (ExceptionInInitializerError ciie) {
				// ignore not instantiable classes
			} catch (UnsatisfiedLinkError ule) {
				// ignore not instantiable classes
			}
		}
	}
	
	// 将完全类名 转换成 类名
	// com.weibo.qa.testcase.strategy.ConfigManageTest  -> ConfigManageTest
	public List<String> classNameList(List<Class<?>> testClass){
		List<String> className = new ArrayList<String>();
		String singleClassStr;
		for(Class<?> singleClass : testClass){
			singleClassStr = singleClass.toString();
			className.add(singleClassStr.substring(singleClassStr.lastIndexOf(".") + 1));
		}
		
		return className;
	}

	
}
