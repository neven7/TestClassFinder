/*
 * @author Johannes Link (business@johanneslink.net)
 * 
 * Published under Apache License, Version 2.0 (http://apache.org/licenses/LICENSE-2.0)
 */
package com.weibo.qa.testclassfinder;

public interface ClassesFinderFactory {
	ClassesFinder create(boolean searchInJars, String[] filterPatterns, SuiteType[] suiteTypes, Class<?>[] baseTypes,
			Class<?>[] excludedBaseTypes, String classpathProperty);
}
