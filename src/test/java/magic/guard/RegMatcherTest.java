package magic.guard;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author xubao
 * @version 1.0
 * @since 2019/3/7
 */
public class RegMatcherTest
{
	@Test
	public void test1()
	{
		RegMatcher regMatcher = new RegMatcher();
		boolean m1 = regMatcher.match("12354");
		assert m1 == false;

		regMatcher.addPattern(".*");
		boolean m2 = regMatcher.match("2343");
		assert m2 == true;
	}

	@Test
	public void test2()
	{
		RegMatcher regMatcher = new RegMatcher(new HashSet<>(Arrays.asList("com/xubao/a/.*")));
		assert regMatcher.match("com/xubao/a/a/1") == true;
		assert regMatcher.match("com/xubao/a") == false;
		assert regMatcher.match("com/xubao/a/3..3") == true;

		regMatcher.addPattern("com/xubao/b/\\d{2,4}-a.class");
		assert regMatcher.match("com/xubao/b/3.class") == false;
		assert regMatcher.match("com/xubao/b/34-a.class") == true;
	}

	@Test
	public void test3()
	{
		RegMatcher regMatcher = new RegMatcher(new HashSet<>(Arrays.asList("com/.*","com/b/.*")));
		Set<Pattern> matchPat = regMatcher.getMatchPat("com/b/123", false);
		System.out.println(matchPat);

		matchPat = regMatcher.getMatchPat("com/b/123", true);
		System.out.println(matchPat);
	}
}
