package magic.guard;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author xubao
 * @version 1.0
 * @since 2019/3/4
 */
public class RegMatcher
{
	private Set<Pattern> patternSet;

	public RegMatcher()
	{
	}

	public RegMatcher(Set<String> regexSet)
	{
		if(regexSet == null)
		{
			return;
		}
		for(String regex : regexSet)
		{
			addPattern(regex);
		}
	}

	public void addPattern(String regex)
	{
		if(patternSet == null)
		{
			patternSet = new HashSet<>();
		}
		Pattern p = Pattern.compile(regex);
		patternSet.add(p);
	}

	public boolean match(String str)
	{
		return !getMatchPat(str, true).isEmpty();
	}

	public Set<Pattern> getMatchPat(String str, boolean once)
	{
		Set<Pattern> set = null;
		if(patternSet != null)
		{
			for(Pattern p : patternSet)
			{
				Matcher matcher = p.matcher(str);
				if(matcher.matches())
				{
					if(set == null)
					{
						set = new HashSet<>();
					}
					set.add(p);

					if(once)
					{
						return set;
					}
				}
			}
		}
		return set != null ? set : Collections.emptySet();
	}
}
