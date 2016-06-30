package com.weibo.qa.testclassfinder.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.weibo.qa.testclassfinder.TestClassFinder;

public class JUnitTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
			// TODO Auto-generated method stub
			String[] filterPatterns = { "com.weibo.qa.testclassfinder.test.*Test" };
			TestClassFinder tcn = new TestClassFinder(filterPatterns);

			System.out.println(tcn.find());
			
			
			System.out.println(tcn.classNameList(tcn.find()));
	}

}
