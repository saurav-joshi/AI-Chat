package com.aibot.utils;


import com.google.common.collect.Sets;
import org.apache.poi.ss.formula.functions.T;

import java.util.Set;

public class StringUtils {

	public static boolean isEmpty(String string) {
		if (string == null) {
			return true;
		}
		if ("".equals(string.trim())) {
			return true;
		}
		return false;
	}

	public static int getLevenshteinDistance(String s, String t) {
		if (s == null || t == null) {
			throw new IllegalArgumentException("Strings must not be null");
		}
		int n = s.length();
		int m = t.length();
		if (n == 0)
			return m;
		else if (m == 0)
			return n;
		if (n > m) {
			String tmp = s;
			s = t;
			t = tmp;
			n = m;
			m = t.length();
		}
		int p[] = new int[n + 1];
		int d[] = new int[n + 1];
		int i, j, cost, _d[];
		char t_j;
		for (i = 0; i <= n; i++)
			p[i] = i;
		for (j = 1; j <= m; j++) {
			t_j = t.charAt(j - 1);
			d[0] = j;
			for (i = 1; i <= n; i++) {
				cost = s.charAt(i - 1) == t_j ? 0 : 1;
				d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
			}
			_d = p;
			p = d;
			d = _d;
		}
		return p[n];
	}

	public static double getJaccardSimilarity(String[] strArray1, String[] strArray2) {
		if (strArray1.length == 0 || strArray2.length == 0) {
			return 0.0;
		}
		Set<String> set1 = Sets.newHashSet(strArray1);
		Set<String> set2 = Sets.newHashSet(strArray2);

		double intersectionSize = Sets.intersection(set1, set2).size();
		double unionSize = Sets.union(set1, set2).size();

		return (intersectionSize / unionSize);
	}
}
