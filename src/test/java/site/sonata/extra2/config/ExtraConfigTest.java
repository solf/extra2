/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.config;

import static org.testng.Assert.assertEquals;
import static site.sonata.extra2.util.NullUtil.fakeNonNull;
import static site.sonata.extra2.util.NullUtil.nnc;
import static site.sonata.extra2.util.NullUtil.nonNull;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.javatuples.Pair;
import org.testng.annotations.Test;

import site.sonata.extra2.config.Configuration;
import site.sonata.extra2.config.FlatConfiguration;
import site.sonata.extra2.config.MergingFlatConfiguration;
import site.sonata.extra2.config.MergingSectionedConfiguration;
import site.sonata.extra2.config.SectionedConfiguration;
import site.sonata.extra2.options.BaseOptions;
import site.sonata.extra2.options.OptionConstraint;
import site.sonata.extra2.util.SplitUtil;

/**
 * Tests for {@link Configuration}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class ExtraConfigTest
{
	/**
	 * Configuration tests.
	 */
	@SuppressWarnings("null")
	@Test
	public void testConfiguration()
	{
		FlatConfiguration cfg;
		SectionedConfiguration iniCfg;
		Set<String> keySet;
		
		// Testing loading configuration as resource bundle.
		{
			cfg = Configuration.fromResourceBundle(ResourceBundle.getBundle("config/rb-rb"));
			assertFlatConfigurationContents(cfg, "prop1", "value1", "prop2", "value2");
			
			try
			{
				cfg = Configuration.fromResourceBundle(null);
				assert false;
			} catch (NullPointerException e)
			{
				// Okay.
			}
			
			try
			{
				cfg = Configuration.fromResourceBundle(ResourceBundle.getBundle("config/rb-rb2"));
				assert false;
			} catch (MissingResourceException e)
			{
				assert e.toString().contains("config/rb-rb2");
			}
			
			// Okay, PropertyResourceBundle behaves very strangely here, but what can we doooo.....
			cfg = Configuration.fromResourceBundle(ResourceBundle.getBundle("config/not-properties-file"));
			assertFlatConfigurationContents(cfg, "not", "a proper properties file this is", "notatall", "");
		}
		
		// Testing loading configuration as properties file name.
		{
			cfg = Configuration.fromPropertiesFile("config/rb-rb");
			assertFlatConfigurationContents(cfg, "prop1", "value1", "prop2", "value2");
			
			// Also test with full name.
			cfg = Configuration.fromPropertiesFile("config/rb-rb.properties");
			assertFlatConfigurationContents(cfg, "prop1", "value1", "prop2", "value2");
			
			try
			{
				cfg = Configuration.fromPropertiesFile((String)null);
				assert false;
			} catch (NullPointerException e)
			{
				// Okay.
			}
			
			try
			{
				cfg = Configuration.fromPropertiesFile("config/rb-rb2");
				assert false;
			} catch (MissingResourceException e)
			{
				assert e.toString().contains("config/rb-rb2");
			}
			
			try
			{
				cfg = Configuration.fromPropertiesFile("config/rb-rb.property");
				assert false;
			} catch (MissingResourceException e)
			{
				assert e.toString().contains("config/rb-rb.property");
			}
			
			// Okay, PropertyResourceBundle behaves very strangely here, but what can we doooo.....
			cfg = Configuration.fromPropertiesFile("config/not-properties-file");
			assertFlatConfigurationContents(cfg, "not", "a proper properties file this is", "notatall", "");
		}
		
		// Testing loading configuration as properties file
		{
			cfg = Configuration.fromPropertiesFile(new File("src/test/resources/config/rb-rb.properties"));
			assertFlatConfigurationContents(cfg, "prop1", "value1", "prop2", "value2");
			
			try
			{
				cfg = Configuration.fromPropertiesFile(new File("src/test/resources/config/rb-rb"));
				assert false;
			} catch (MissingResourceException e)
			{
				assert e.toString().contains("java.io.FileNotFoundException");
			}
			
			try
			{
				cfg = Configuration.fromPropertiesFile((File)null);
				assert false;
			} catch (NullPointerException e)
			{
				// Okay.
			}
		}
		
		// Test ini loading via file name on classpath.
		{
			iniCfg = Configuration.fromIniFile("config/ini-file.ini");
			keySet = toHashSet(iniCfg.getAllSectionKeys());
			assert keySet.size() == 2;
			assert keySet.contains("section1");
			assert keySet.contains("section2");
			
			cfg = iniCfg.getGlobalSection();
			assertFlatConfigurationContents(cfg, "global1", "gval1", "global2", "gval2");
			
			cfg = iniCfg.getSection("section1");
			assertFlatConfigurationContents(cfg, "section1", "s1val1", "section2", "s1val2");
			
			cfg = iniCfg.getSection("section2");
			assertFlatConfigurationContents(cfg, "section1", "s2val1", "section2", "s2val2");
			
			// Test loading of file with no global section.
			iniCfg = Configuration.fromIniFile("config/ini-file-empty-global.ini");
			keySet = toHashSet(iniCfg.getAllSectionKeys());
			assert keySet.size() == 2;
			assert keySet.contains("section1");
			assert keySet.contains("section2");
			
			cfg = iniCfg.getGlobalSection();
			assert toHashSet(cfg.getAllKeys()).size() == 0;
			
			cfg = iniCfg.getSection("section1");
			assertFlatConfigurationContents(cfg, "section1", "s1val1", "section2", "s1val2");
			
			cfg = iniCfg.getSection("section2");
			assertFlatConfigurationContents(cfg, "section1", "s2val1", "section2", "s2val2");
			
			try
			{
				cfg = iniCfg.getSection(null);
				assert false;
			} catch (NullPointerException e)
			{
				// Okay.
			}
			
			try
			{
				iniCfg = Configuration.fromIniFile((String)null);
				assert false;
			} catch (NullPointerException e)
			{
				// Okay.
			}
			
			try
			{
				iniCfg = Configuration.fromIniFile("config/ini-file-2.ini");
				assert false;
			} catch (MissingResourceException e)
			{
				assert e.toString().contains("config/ini-file-2.ini");
			}
			
			try
			{
				iniCfg = Configuration.fromIniFile("config/ini-file");
				assert false;
			} catch (MissingResourceException e)
			{
				assert e.toString().contains("config/ini-file");
			}
			
			// Okay, Apache configuration system behaves differently from resource bundle here, but what can we doooo.....
			iniCfg = Configuration.fromIniFile("config/not-properties-file.properties");
			assert toHashSet(iniCfg.getAllSectionKeys()).size() == 0;
			cfg = iniCfg.getGlobalSection();
			assertFlatConfigurationContents(cfg, "not a proper properties file this is", "", "notatall", "");
		}
		
		// Testing loading ini as file
		{
			iniCfg = Configuration.fromIniFile(new File("src/test/resources/config/ini-file.ini"));
			keySet = toHashSet(iniCfg.getAllSectionKeys());
			assert keySet.size() == 2;
			assert keySet.contains("section1");
			assert keySet.contains("section2");
			
			cfg = iniCfg.getGlobalSection();
			assertFlatConfigurationContents(cfg, "global1", "gval1", "global2", "gval2");
			
			cfg = iniCfg.getSection("section1");
			assertFlatConfigurationContents(cfg, "section1", "s1val1", "section2", "s1val2");
			
			cfg = iniCfg.getSection("section2");
			assertFlatConfigurationContents(cfg, "section1", "s2val1", "section2", "s2val2");
			
			try
			{
				iniCfg = Configuration.fromIniFile(new File("src/test/resources/config/ini-file"));
				assert false;
			} catch (MissingResourceException e)
			{
				assert e.toString().contains("Unable to load configuration from file");
			}
			
			try
			{
				iniCfg = Configuration.fromIniFile((File)null);
				assert false;
			} catch (NullPointerException e)
			{
				// Okay.
			}
		}
	}
	
	/**
	 * Converts iterable to hash set for convenient testing.
	 */
	private Set<String> toHashSet(Iterable<String> iterable)
	{
		Set<String> result = new HashSet<String>();
		for (String str : iterable)
		{
			result.add(str);
		}
		
		return result;
	}
	
	/**
	 * Asserts that particular {@link FlatConfiguration} contains these key-value pairs
	 * and nothing else.
	 */
	private void assertFlatConfigurationContents(FlatConfiguration cfg, String... keyValuePairs)
	{
		assert keyValuePairs.length > 0;
		assert keyValuePairs.length % 2 == 0;

		Set<String> keySet = toHashSet(cfg.getAllKeys());
		assert keySet.size() == keyValuePairs.length / 2;
		
		for (int i = 0; i < keyValuePairs.length; i += 2)
		{
			assert cfg.getString(nonNull(keyValuePairs[i])).equals(keyValuePairs[i + 1]);
			assert keySet.remove(keyValuePairs[i]);
		}
		
		assert keySet.size() == 0;
		
		
		try
		{
			cfg.getString("not-an-existing-property-name-definitely");
			assert false;
		} catch (MissingResourceException e)
		{
			assert e.toString().contains("not-an-existing-property-name-definitely");
		}
	}
	
	/**
	 * Tests for int pairs.
	 */
	@Test
	public void testIntPair()
	{
		// Int pairs test.
		{
			BaseOptions options = new BaseOptions(Configuration.fromPropertiesFile("config/int-pair.properties"));
			
			try
			{
				options.getIntPair("prop1");
				assert false;
			} catch (IllegalArgumentException e)
			{
				assert e.toString().contains("value doesn't parse as a pair");
			}
			
			try
			{
				options.getIntPair("propAsd");
				assert false;
			} catch (IllegalArgumentException e)
			{
				assert e.toString().contains("value doesn't parse as a pair");
			}
			
			try
			{
				options.getIntPair("propPair");
				assert false;
			} catch (NumberFormatException e)
			{
				// Okay
			}
			
			try
			{
				options.getIntPairNegOneOrMore("propIntNeg1Pair");
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.toString().contains("value must have -1 or more for both integers");
			}
			
			try
			{
				options.getIntPairNegOneOrMore("propIntNeg2Pair");
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.toString().contains("value must have -1 or more for both integers");
			}
			
			{
				Pair<Integer, Integer> pair = options.getIntPair("propIntNeg1Pair");
				assert pair.getValue0() == -3;
				assert pair.getValue1() == 5;
			}
			
			{
				Pair<Integer, Integer> pair = options.getIntPair("propIntNeg2Pair");
				assert pair.getValue0() == 3;
				assert pair.getValue1() == -5;
			}
			
			{
				Pair<Integer, Integer> pair = options.getIntPairNegOneOrMore("propIntPosPair");
				assert pair.getValue0() == 3;
				assert pair.getValue1() == 7;
			}
			
			{
				Pair<Integer, Integer> pair = options.getIntPairNegOneOrMore("propIntZeroPair");
				assert pair.getValue0() == 0;
				assert pair.getValue1() == 0;
			}
			
		}
	}
	
	/**
	 * Time interval tests.
	 */
	@Test
	public void testTimeIntervals1()
	{
		BaseOptions options = new BaseOptions(Configuration.fromPropertiesFile("config/time.properties"));
		
		assert options.getTimeInterval("millisecond") == 1;
		assert options.getTimeInterval("zerosec") == 0;
		assert options.getTimeInterval("second5") == 5l * 1000; 
		assert options.getTimeInterval("minutes63") == 63l * 60 * 1000; 
		assert options.getTimeInterval("hours124") == 124l * 60 * 60 * 1000; 
		assert options.getTimeInterval("days1111") == 1111l * 24 * 60 * 60 * 1000;
		
		try
		{
			options.getTimeInterval("empty");
			assert false;
		} catch (MissingResourceException e)
		{
			// ok.
		}
		
		try
		{
			options.getTimeInterval("nodigits");
			assert false;
		} catch (IllegalArgumentException e)
		{
			// Ok
		}
		
		try
		{
			options.getTimeInterval("noletters");
			assert false;
		} catch (IllegalArgumentException e)
		{
			// Ok
		}
		
		try
		{
			options.getTimeInterval("wrongletters");
			assert false;
		} catch (IllegalArgumentException e)
		{
			// Ok
		}
	}

	/**
	 * Tests time interval option handling.
	 */
	@SuppressWarnings("null")
	@Test
	public void testTimeIntervals2()
	{
		BaseOptions options = new BaseOptions(new MirrorFlatConfiguration("config/empty"));
		
		// First test failures.
		try
		{
			options.getTimeInterval(null);
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Option must be non-null");
		}
		
		try
		{
			options.getTimeInterval(MirrorFlatConfiguration.NULL_PREFIX + "option");
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Missing option");
		}
		
		try
		{
			options.getTimeInterval("");
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Empty option value");
		}
		
		try
		{
			options.getTimeInterval("1");
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Failed to parse option");
			assert e.toString().contains("Cannot parse value as time interval:");
		}
		
		try
		{
			options.getTimeInterval("12");
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Failed to parse option");
			assert e.toString().contains("Cannot parse value as time interval:");
		}
		
		try
		{
			options.getTimeInterval("s");
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Failed to parse option");
			assert e.toString().contains("Cannot parse value as time interval:");
		}
		
		try
		{
			options.getTimeInterval("ms");
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Failed to parse option");
			assert e.toString().contains("Cannot parse value as time interval:");
		}
		
		try
		{
			options.getTimeInterval("gibberish");
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Failed to parse option");
			assert e.toString().contains("Cannot parse value as time interval:");
		}
		
		try
		{
			options.getTimeInterval("1y");
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Failed to parse option");
			assert e.toString().contains("Cannot parse value as time interval:");
		}
		
		try
		{
			options.getTimeInterval("1.1ms");
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Failed to parse option");
			assert e.toString().contains("Cannot parse value as time interval:");
		}
		
		try
		{
			options.getTimeInterval("3S");
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Failed to parse option");
			assert e.toString().contains("Cannot parse value as time interval:");
		}
		
		try
		{
			options.getTimeInterval("-3ms");
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Failed to parse option");
			assert e.toString().contains("Cannot parse value as time interval");
			assert e.toString().contains("must not be negative");
		}
		
		try
		{
			// variant with invalid default value and present option
			options.getTimeInterval("1ms", "invalid");
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Failed to parse option");
			assert e.toString().contains("Cannot parse value as time interval");
			assert e.toString().contains("DEFAULT value as time interval");
			assert e.toString().contains("Cannot parse value as time interval: invalid");
		}
		
		try
		{
			// variant with invalid default value and missing option value
			options.getTimeInterval(MirrorFlatConfiguration.NULL_PREFIX, "invalid");
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Failed to parse option");
			assert e.toString().contains("Cannot parse value as time interval");
			assert e.toString().contains("DEFAULT value as time interval");
			assert e.toString().contains("Cannot parse value as time interval: invalid");
		}
		
		try
		{
			// variant with invalid option value and valid default
			options.getTimeInterval("invalid", "1s");
			assert false;
		} catch (Exception e)
		{
			assert e.toString().contains("Failed to parse option");
			assert e.toString().contains("Cannot parse value as time interval");
			assert e.toString().contains("value as time interval");
			assert e.toString().contains("Cannot parse value as time interval: invalid");
		}
		
		// Now test that values parse properly.
		assert options.getTimeInterval("0ms") == 0;
		assert options.getTimeInterval("1ms") == 1;
		assert options.getTimeInterval("35ms") == 35;
		assert options.getTimeInterval("0s") == 0;
		assert options.getTimeInterval("1s") == 1 * 1000;
		assert options.getTimeInterval("35s") == 35 * 1000;
		assert options.getTimeInterval("0m") == 0;
		assert options.getTimeInterval("1m") == 1 * 1000 * 60;
		assert options.getTimeInterval("35m") == 35 * 1000 * 60;
		assert options.getTimeInterval("0h") == 0;
		assert options.getTimeInterval("1h") == 1 * 1000 * 60 * 60;
		assert options.getTimeInterval("35h") == 35 * 1000 * 60 * 60;
		assert options.getTimeInterval("0d") == 0;
		assert options.getTimeInterval("1d") == 1 * 1000 * 60 * 60 * 24;
		assert options.getTimeInterval("35d") == 35l * 1000 * 60 * 60 * 24;

		// Test defaults
		assert options.getTimeInterval("35d", "1s") == 35l * 1000 * 60 * 60 * 24;
		assert options.getTimeInterval(MirrorFlatConfiguration.NULL_PREFIX, "1s") == 1 * 1000;
	}
	
	/**
	 * Tests various timeIntervalPositive methods.
	 */
	@Test
	public void testTimeIntervalsPositive()
	{
		BaseOptions options = new BaseOptions(Configuration.fromPropertiesFile("config/time.properties"));
		
		assert options.getTimeIntervalPositive("millisecond") == 1;
		assert options.getTimeIntervalPositive("second5", -3) == 5l * 1000; 
		assert options.getTimeIntervalPositive("minutes63", "7s") == 63l * 60 * 1000; 
		assert options.getTimeIntervalPositiveOrNull("hours124") == 124l * 60 * 60 * 1000; 
		assert options.getTimeIntervalPositive("days1111") == 1111l * 24 * 60 * 60 * 1000;

		assert options.getTimeIntervalPositive("notexists-second5", -3) == -3; 
		assert options.getTimeIntervalPositive("notexists-minutes63", "7s") == 7 * 1000; 
		assert options.getTimeIntervalPositiveOrNull("notexists-hours124") == null; 
		
		try
		{
			options.getTimeIntervalPositive("zerosec");
			assert false;
		} catch (IllegalStateException e)
		{
			// Ok
		}
		
		try
		{
			options.getTimeIntervalPositive("zerosec", -3);
			assert false;
		} catch (IllegalStateException e)
		{
			// Ok
		}
		
		try
		{
			options.getTimeIntervalPositive("zerosec", "28m");
			assert false;
		} catch (IllegalStateException e)
		{
			// Ok
		}
		
		try
		{
			options.getTimeIntervalPositiveOrNull("zerosec");
			assert false;
		} catch (IllegalStateException e)
		{
			// Ok
		}

	}
	
	/**
	 * Test key-value pairs.
	 */
	@Test
	public void testKeyValueAsString()
	{
		BaseOptions options = new BaseOptions(Configuration.fromPropertiesFile("config/keyValueAsString.properties"));
		
		{
			Map<String, String> data = options.getKeyValueAsString("one");
			assert data.size() == 1;
			assert "value1".equals(data.get("key1"));
		}
		{
			Map<String, String> data = options.getKeyValueAsString("oneEndDelim");
			assert data.size() == 1;
			assert "value1".equals(data.get("key1"));
		}
		{
			Map<String, String> data = options.getKeyValueAsString("oneStartDelim");
			assert data.size() == 1;
			assert "value1".equals(data.get("key1"));
		}
		{
			Map<String, String> data = options.getKeyValueAsString("oneBothDelim");
			assert data.size() == 1;
			assert "value1".equals(data.get("key1"));
		}
		{
			Map<String, String> data = options.getKeyValueAsString("two");
			assert data.size() == 2;
			assert "value1".equals(data.get("key1"));
			assert "value2".equals(data.get("key2"));
		}
		{
			try
			{
				options.getKeyValueAsString("noKey");
				assert false;
			} catch (IllegalArgumentException e)
			{
				assert e.toString().contains(" :value;") : e.toString();
			}
		}
		{
			Map<String, String> data = options.getKeyValueAsString("noValue");
			assert data.size() == 1;
			assert "".equals(data.get("key"));
		}
		{
			try
			{
				options.getKeyValueAsString("noDelim");
				assert false;
			} catch (IllegalArgumentException e)
			{
				assert e.toString().contains(" something;") : e.toString();
			}
		}
		{
			try
			{
				options.getKeyValueAsString("noColon");
				assert false;
			} catch (IllegalArgumentException e)
			{
				assert e.toString().contains(" something;") : e.toString();
			}
		}
		{
			Map<String, String> data = options.getKeyValueAsString("empty");
			assert data.size() == 0;
		}
		{
			Map<String, String> data = options.getKeyValueAsString("missing", "keyD:valueD");
			assert data.size() == 1;
			assert "valueD".equals(data.get("keyD"));
		}
		{
			Map<String, String> data = options.getKeyValueAsString("empty", "keyD:valueD");
			assert data.size() == 0;
		}
	}
	
	/**
	 * Tests {@link MergingFlatConfiguration}
	 */
	@Test
	public void testMergingFlatConfiguration()
	{
		@Nonnull FlatConfiguration c1 = Configuration.fromPropertiesFile("config/merge1");
		@Nonnull FlatConfiguration c2 = Configuration.fromPropertiesFile("config/merge2");
		
		@Nonnull FlatConfiguration cfg = Configuration.merge(c1, c2);
		
		assert "value1".equals(cfg.getString("merge01"));
		assert "value1".equals(cfg.getString("merge11"));
		assert "value2".equals(cfg.getString("merge21"));
		
		try
		{
			cfg.getString("asd");
			assert false;
		} catch (MissingResourceException e)
		{
			// expected
		}
		
		@Nonnull Iterable<@Nonnull String> sKeys = cfg.getAllKeys();
		HashSet<@Nonnull String> keys = new HashSet<>();
		for (@Nonnull String key : sKeys)
			assert keys.add(key) : key;
			
		assert keys.remove("merge01");
		assert keys.remove("merge11");
		assert keys.remove("merge21");
		assert keys.isEmpty();
	}
	
	/**
	 * Tests {@link MergingSectionedConfiguration}
	 */
	@Test
	public void testMergingSectionedConfiguration()
	{
		SectionedConfiguration base = Configuration.fromIniFile("config/mergeBase.ini");
		SectionedConfiguration override = Configuration.fromIniFile("config/mergeOverride.ini");
		
		SectionedConfiguration cfg = Configuration.merge(override, base);
		
		{
			// Tests sections list.
			Set<String> sections = new HashSet<>();
			for (String section : cfg.getAllSectionKeys())
				assert sections.add(section);
			
			assert sections.remove("base");
			assert sections.remove("override");
			assert sections.remove("all");
			assert sections.isEmpty();
		}
		
		{
			// Tests missing section.
			try
			{
				cfg.getSection("missing");
				assert false; // should not happen
			} catch (MissingResourceException e)
			{
				// expected
			}
			try
			{
				cfg.getSection(fakeNonNull());
				assert false; // should not happen
			} catch (NullPointerException e)
			{
				// expected
			}
		}
		
		{
			// Tests global section
			FlatConfiguration section = cfg.getGlobalSection();
			
			Set<String> keys = new HashSet<>();
			for (String key : section.getAllKeys())
				assert keys.add(key) : key;
			assert keys.remove("glBase");	
			assert keys.remove("glAll");	
			assert keys.remove("glOverride");
			assert keys.isEmpty();
			
			try
			{
				assert section.getString("missing") == null;
				assert false; // must not get here
			} catch (MissingResourceException e)
			{
				// expected
			}
			try
			{
				assert section.getString(fakeNonNull()) == null;
				assert false; // must not get here
			} catch (NullPointerException e)
			{
				// expected
			}
			
			assertEquals(section.getString("glBase"), "vGlBase");
			assertEquals(section.getString("glAll"), "vGlOverrideAll");
			assertEquals(section.getString("glOverride"), "vGlOverride");
		}
		
		{
			// Tests base section
			FlatConfiguration section = cfg.getSection("base");
			
			Set<String> keys = new HashSet<>();
			for (String key : section.getAllKeys())
				assert keys.add(key) : key;
			assert keys.remove("bBase");	
			assert keys.isEmpty();
			
			try
			{
				assert section.getString("missing") == null;
				assert false; // must not get here
			} catch (MissingResourceException e)
			{
				// expected
			}
			try
			{
				assert section.getString(fakeNonNull()) == null;
				assert false; // must not get here
			} catch (NullPointerException e)
			{
				// expected
			}
			
			assertEquals(section.getString("bBase"), "vBBase");
		}
		
		{
			// Tests override section
			FlatConfiguration section = cfg.getSection("override");
			
			Set<String> keys = new HashSet<>();
			for (String key : section.getAllKeys())
				assert keys.add(key) : key;
			assert keys.remove("oOveride");	
			assert keys.isEmpty();
			
			try
			{
				assert section.getString("missing") == null;
				assert false; // must not get here
			} catch (MissingResourceException e)
			{
				// expected
			}
			try
			{
				assert section.getString(fakeNonNull()) == null;
				assert false; // must not get here
			} catch (NullPointerException e)
			{
				// expected
			}
			
			assertEquals(section.getString("oOveride"), "vOOveride");
		}
		
		{
			// Tests shared section
			FlatConfiguration section = cfg.getSection("all");
			
			Set<String> keys = new HashSet<>();
			for (String key : section.getAllKeys())
				assert keys.add(key) : key;
			assert keys.remove("aBase");	
			assert keys.remove("aAll");	
			assert keys.remove("aOverride");
			assert keys.isEmpty();
			
			try
			{
				assert section.getString("missing") == null;
				assert false; // must not get here
			} catch (MissingResourceException e)
			{
				// expected
			}
			try
			{
				assert section.getString(fakeNonNull()) == null;
				assert false; // must not get here
			} catch (NullPointerException e)
			{
				// expected
			}
			
			assertEquals(section.getString("aBase"), "aBase");
			assertEquals(section.getString("aOverride"), "vAOverride");
			assertEquals(section.getString("aAll"), "aOverrideAll");
		}
	}
	
	/**
	 * Tests {@link SplitUtil}
	 */
	@Test
	public void testSplitUtil()
	{
		// Start with encode/decode test.
		{
			String str = "";
			String encoded = SplitUtil.encodeString(str);
			assert encoded.length() > 0;
			assert SplitUtil.decodeString(encoded).equals(str);
		}
		{
			String str = "a";
			String encoded = SplitUtil.encodeString(str);
			assert encoded.length() > 0;
			assert SplitUtil.decodeString(encoded).equals(str);
		}
		{
			String str = "1";
			String encoded = SplitUtil.encodeString(str);
			assert encoded.length() > 0;
			assert SplitUtil.decodeString(encoded).equals(str);
		}
		{
			String str = "1.a";
			String encoded = SplitUtil.encodeString(str);
			assert encoded.length() > 0;
			assert SplitUtil.decodeString(encoded).equals(str);
		}
		{
			String str = ".";
			String encoded = SplitUtil.encodeString(str);
			assert encoded.length() > 0;
			assert SplitUtil.decodeString(encoded).equals(str);
		}
		{
			String str = null;
			try
			{
				SplitUtil.encodeString(fakeNonNull(str));
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			String encoded = "";
			try
			{
				SplitUtil.decodeString(encoded);
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			String encoded = ".";
			try
			{
				SplitUtil.decodeString(encoded);
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			String encoded = "..";
			try
			{
				SplitUtil.decodeString(encoded);
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			String encoded = null;
			try
			{
				SplitUtil.decodeString(fakeNonNull(encoded));
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			String encoded = "1";
			try
			{
				SplitUtil.decodeString(encoded);
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			String encoded = "1.";
			try
			{
				SplitUtil.decodeString(encoded);
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			String encoded = "2.a";
			try
			{
				SplitUtil.decodeString(encoded);
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			String encoded = "1.as";
			try
			{
				SplitUtil.decodeString(encoded);
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		
		// Now appendHead / splitHead
		{
			String head = "head";
			String tail = "tail";
			char delim = '_';
			String encoded = SplitUtil.appendHead(head, delim, tail);
			assert encoded.length() > 0;
			String[] split = SplitUtil.splitHead(encoded);
			assert split[0].equals(head);
			assert split[1].equals(tail);
		}
		{
			String head = "a";
			String tail = "a";
			char delim = 'a';
			String encoded = SplitUtil.appendHead(head, delim, tail);
			assert encoded.length() > 0;
			String[] split = SplitUtil.splitHead(encoded);
			assert split[0].equals(head);
			assert split[1].equals(tail);
		}
		{
			String head = "";
			String tail = "";
			char delim = ' ';
			String encoded = SplitUtil.appendHead(head, delim, tail);
			assert encoded.length() > 0;
			String[] split = SplitUtil.splitHead(encoded);
			assert split[0].equals(head);
			assert split[1].equals(tail);
		}
		{
			String head = "";
			String tail = "a";
			char delim = ' ';
			String encoded = SplitUtil.appendHead(head, delim, tail);
			assert encoded.length() > 0;
			String[] split = SplitUtil.splitHead(encoded);
			assert split[0].equals(head);
			assert split[1].equals(tail);
		}
		{
			String head = "a";
			String tail = "";
			char delim = ' ';
			String encoded = SplitUtil.appendHead(head, delim, tail);
			assert encoded.length() > 0;
			String[] split = SplitUtil.splitHead(encoded);
			assert split[0].equals(head);
			assert split[1].equals(tail);
		}
		{
			String head = "aa";
			String tail = "a";
			char delim = 'a';
			String encoded = SplitUtil.appendHead(head, delim, tail);
			assert encoded.length() > 0;
			String[] split = SplitUtil.splitHead(encoded);
			assert split[0].equals(head);
			assert split[1].equals(tail);
		}
		{
			String head = "a";
			String tail = "aa";
			char delim = 'a';
			String encoded = SplitUtil.appendHead(head, delim, tail);
			assert encoded.length() > 0;
			String[] split = SplitUtil.splitHead(encoded);
			assert split[0].equals(head);
			assert split[1].equals(tail);
		}
		{
			assert !SplitUtil.appendHead("a", 'a', "aa").equals(SplitUtil.appendHead("aa", 'a', "a"));
			assert !SplitUtil.appendHead("a", 'a', "aa").equals(SplitUtil.appendHead("1.a", 'a', "a"));
		}
		{
			try
			{
				SplitUtil.appendHead(fakeNonNull((String)null), 'a', "a");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.appendHead("a", 'a', fakeNonNull((String)null));
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.appendHead(fakeNonNull((String)null), 'a', fakeNonNull((String)null));
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitHead("1.a");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitHead(fakeNonNull((String)null));
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitHead("");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitHead("asd");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitHead("3.as:");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitHead("3");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitHead("33");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitHead(".");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitHead("..");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		
		// Now test strings merge / split.
		{
			String s1 = "one";
			String s2 = "two";
			String s3 = "three";
			char delim = '_';
			String encoded = SplitUtil.mergeStrings(delim, s1, s2, s3);
			assert encoded.length() > 0;
			String[] split = SplitUtil.splitStrings(encoded);
			assert split[0].equals(s1);
			assert split[1].equals(s2);
			assert split[2].equals(s3);
		}
		{
			String s1 = "";
			String s2 = "";
			String s3 = "";
			char delim = '_';
			String encoded = SplitUtil.mergeStrings(delim, s1, s2, s3);
			assert encoded.length() > 0;
			String[] split = SplitUtil.splitStrings(encoded);
			assert split[0].equals(s1);
			assert split[1].equals(s2);
			assert split[2].equals(s3);
		}
		{
			String s1 = "a";
			String s2 = "a";
			String s3 = "a";
			char delim = 'a';
			String encoded = SplitUtil.mergeStrings(delim, s1, s2, s3);
			assert encoded.length() > 0;
			String[] split = SplitUtil.splitStrings(encoded);
			assert split[0].equals(s1);
			assert split[1].equals(s2);
			assert split[2].equals(s3);
		}
		{
			String s1 = "a";
			String s2 = "";
			String s3 = "";
			char delim = 'a';
			String encoded = SplitUtil.mergeStrings(delim, s1, s2, s3);
			assert encoded.length() > 0;
			String[] split = SplitUtil.splitStrings(encoded);
			assert split[0].equals(s1);
			assert split[1].equals(s2);
			assert split[2].equals(s3);
		}
		{
			String s1 = "";
			String s2 = "a";
			String s3 = "";
			char delim = 'a';
			String encoded = SplitUtil.mergeStrings(delim, s1, s2, s3);
			assert encoded.length() > 0;
			String[] split = SplitUtil.splitStrings(encoded);
			assert split[0].equals(s1);
			assert split[1].equals(s2);
			assert split[2].equals(s3);
		}
		{
			String s1 = "";
			String s2 = "";
			String s3 = "a";
			char delim = 'a';
			String encoded = SplitUtil.mergeStrings(delim, s1, s2, s3);
			assert encoded.length() > 0;
			String[] split = SplitUtil.splitStrings(encoded);
			assert split[0].equals(s1);
			assert split[1].equals(s2);
			assert split[2].equals(s3);
		}
		{
			String s1 = "1.a";
			String s2 = "2.bb";
			String s3 = "3.ccc";
			char delim = '7';
			String encoded = SplitUtil.mergeStrings(delim, s1, s2, s3);
			assert encoded.length() > 0;
			String[] split = SplitUtil.splitStrings(encoded);
			assert split[0].equals(s1);
			assert split[1].equals(s2);
			assert split[2].equals(s3);
		}
		{
			assert !SplitUtil.mergeStrings('a', "aa", "a", "").equals(SplitUtil.mergeStrings('a', "a", "aa", ""));
			assert !SplitUtil.mergeStrings('a', "aa", "a", "aaa").equals(SplitUtil.mergeStrings('a', "a", "aa", "aaa"));
		}
		{
			try
			{
				SplitUtil.mergeStrings('a', "a");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.mergeStrings('a', "a", fakeNonNull(null));
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.mergeStrings('a', (String)fakeNonNull(null), "a");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.mergeStrings('a', (String)fakeNonNull(null), fakeNonNull(null));
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitStrings("1.a");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitStrings(fakeNonNull((String)null));
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitStrings("");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitStrings("asd");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitStrings("3.as:");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitStrings("3");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitStrings("33");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitStrings(".");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		{
			try
			{
				SplitUtil.splitStrings("..");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
		}
		
		// Test strings merge/split with less than 2 args.
		{
			try
			{
				SplitUtil.mergeStrings(':', "string1");
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
			
			String result = SplitUtil.mergeStrings(':', false, "string1");
			assert result.equals("7.string1") : result;
			
			try
			{
				SplitUtil.splitStrings(result);
				assert false;
			} catch (IllegalArgumentException e)
			{
				// ok
			}
			
			String[] parts = SplitUtil.splitStrings(result, false);
			assert parts.length == 1;
			assert parts[0].equals("string1");
		}
	}
	
	/**
	 * Test long lists and sets
	 */
	@Test
	public void testLongListAndSet()
	{
		BaseOptions options = new BaseOptions(Configuration.fromPropertiesFile("config/longList.properties"));
		
		{
			@Nonnull List<@Nonnull Long> list = options.getLongList("empty");
			assert list.size() == 0;
		}
		
		{
			@Nonnull List<@Nonnull Long> list = options.getLongList("oneLong");
			assert list.size() == 1;
			assert list.get(0) == 1234567890123456l;
			
			// Test unmodifiability
			try
			{
				list.remove(0);
				assert false;
			} catch (UnsupportedOperationException e)
			{
				// ok
			}
			try
			{
				list.add(1l);
				assert false;
			} catch (UnsupportedOperationException e)
			{
				// ok
			}
			try
			{
				list.clear();
				assert false;
			} catch (UnsupportedOperationException e)
			{
				// ok
			}
		}
		
		{
			@Nonnull List<@Nonnull Long> list = options.getLongList("twoLong");
			assert list.size() == 2;
			assert list.get(0) == 123l;
			assert list.get(1) == 1234567890123456l;
		}
		
		{
			@Nonnull List<@Nonnull Long> list = options.getLongList("withZero");
			assert list.size() == 2;
			assert list.get(0) == 1;
			assert list.get(1) == 0;
		}
		
		{
			@Nonnull List<@Nonnull Long> list = options.getLongList("withNegative");
			assert list.size() == 2;
			assert list.get(0) == 5;
			assert list.get(1) == -7;
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Long> list = options.getLongList("missing");
				assert false;
			} catch (MissingResourceException e)
			{
				assert "Missing option: missing".equals(e.getMessage()) : e.getMessage();
			}
		}
		
		{
			@Nonnull List<@Nonnull Long> list = options.getLongList("defaultValue", "123,1234567890123456");
			assert list.size() == 2;
			assert list.get(0) == 123l;
			assert list.get(1) == 1234567890123456l;
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Long> list = options.getLongList("letters");
				assert false;
			} catch (NumberFormatException e)
			{
				assert e.getMessage().contains("Problem with option [letters]: failed to parse [asd] as long in: [asd]") : e.getMessage();
			}
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Long> list = options.getLongList("float");
				assert false;
			} catch (NumberFormatException e)
			{
				assert e.getMessage().contains("Problem with option [float]: failed to parse [1.34] as long in: [1.34]") : e.getMessage();
			}
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Long> list = options.getLongList("emptyListItem");
				assert false;
			} catch (NumberFormatException e)
			{
				assert e.getMessage().contains("Problem with option [emptyListItem]: failed to parse [] as long in: [123, , 456]") : e.getMessage();
			}
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Long> list = options.getLongList("empty", OptionConstraint.NON_EMPTY_COLLECTION);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Collection is empty in option: empty") : e.getMessage();
			}
		}
		
		{
			@Nonnull List<@Nonnull Long> list = options.getLongList("twoLong", OptionConstraint.NON_EMPTY_COLLECTION, 
				OptionConstraint.POSITIVE, OptionConstraint.NON_NEGATIVE, OptionConstraint.NEGATIVE_ONE_OR_MORE);
			assert list.size() == 2;
			assert list.get(0) == 123l;
			assert list.get(1) == 1234567890123456l;
		}
		
		{
			@Nonnull List<@Nonnull Long> list = options.getLongList("singleZero", OptionConstraint.NON_EMPTY_COLLECTION, 
				OptionConstraint.NON_NEGATIVE, OptionConstraint.NEGATIVE_ONE_OR_MORE);
			assert list.size() == 1;
			assert list.get(0) == 0l;
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Long> list = options.getLongList("singleZero", OptionConstraint.NON_EMPTY_COLLECTION, 
					OptionConstraint.POSITIVE, OptionConstraint.NON_NEGATIVE, OptionConstraint.NEGATIVE_ONE_OR_MORE);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Option [singleZero] value [0] violates constraint: POSITIVE") : e.getMessage();
			}
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Long> list = options.getLongList("withZero", OptionConstraint.NON_EMPTY_COLLECTION, 
					OptionConstraint.POSITIVE);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Option [withZero] value [0] violates constraint: POSITIVE") : e.getMessage();
			}
		}
		
		{
			@Nonnull List<@Nonnull Long> list = options.getLongList("withZero", OptionConstraint.NON_EMPTY_COLLECTION, 
				OptionConstraint.NON_NEGATIVE);
			assert list.size() == 2;
			assert list.get(0) == 1l;
			assert list.get(1) == 0l;
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Long> list = options.getLongList("withMinusOne", OptionConstraint.NON_EMPTY_COLLECTION, 
					OptionConstraint.NON_NEGATIVE);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Option [withMinusOne] value [-1] violates constraint: NON_NEGATIVE") : e.getMessage();
			}
		}
		
		{
			@Nonnull List<@Nonnull Long> list = options.getLongList("withMinusOne", OptionConstraint.NON_EMPTY_COLLECTION, 
				OptionConstraint.NEGATIVE_ONE_OR_MORE);
			assert list.size() == 2;
			assert list.get(0) == -1l;
			assert list.get(1) == 3l;
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Long> list = options.getLongList("withNegative", OptionConstraint.NON_EMPTY_COLLECTION, 
					OptionConstraint.NEGATIVE_ONE_OR_MORE);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Option [withNegative] value [-7] violates constraint: NEGATIVE_ONE_OR_MORE") : e.getMessage();
			}
		}
		
		{
			// Test unapplicable constraint + list
			try
			{
				options.getLongList("withNegative", OptionConstraint.NON_EMPTY_ELEMENT);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Option [withNegative] constraint type not applicable to number collection: NON_EMPTY_ELEMENT") : e.getMessage();
			}
		}
		
		{
			// Test unapplicable constraint + set
			try
			{
				options.getLongSet("withNegative", OptionConstraint.NON_EMPTY_ELEMENT);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Option [withNegative] constraint type not applicable to number collection: NON_EMPTY_ELEMENT") : e.getMessage();
			}
		}
		
		{
			// Test missing set without default option.
			try
			{
				options.getLongSet("missing");
				assert false;
			} catch (MissingResourceException e)
			{
				assert e.getMessage().contains("Missing option: missing") : e.getMessage();
			}
		}
		
		{
			// Test missing set with default option.
			@Nonnull Set<@Nonnull Long> set = options.getLongSet("missing", "1,2");
			assert set.size() == 2 : set.size();
			assert set.contains(1l);
			assert set.contains(2l);
		}
		
		{
			// Test simple success scenario.
			@Nonnull Set<@Nonnull Long> set = options.getLongSet("twoLong");
			assert set.size() == 2 : set.size();
			assert set.contains(123l);
			assert set.contains(1234567890123456l);
		}
		
		{
			// Test duplicates
			try
			{
				options.getLongSet("duplicates");
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Duplicate value [1234567890123456]: in option: duplicates") : e.getMessage();
			}
		}
	}
	
	/**
	 * Test int lists & sets
	 */
	@Test
	public void testIntListAndSet()
	{
		BaseOptions options = new BaseOptions(Configuration.fromPropertiesFile("config/intList.properties"));
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Integer> list = options.getIntList("oneLong");
				assert false;
			} catch (NumberFormatException e)
			{
				assert e.getMessage().contains("Problem with option [oneLong]: failed to parse [1234567890123456] as int in: [1234567890123456]") : e.getMessage();
			}
		}
		
		{
			@Nonnull List<@Nonnull Integer> list = options.getIntList("empty");
			assert list.size() == 0;
		}
		
		{
			@Nonnull List<@Nonnull Integer> list = options.getIntList("oneInt");
			assert list.size() == 1;
			assert list.get(0) == 123456789;
			
			// Test unmodifiability
			try
			{
				list.remove(0);
				assert false;
			} catch (UnsupportedOperationException e)
			{
				// ok
			}
			try
			{
				list.add(1);
				assert false;
			} catch (UnsupportedOperationException e)
			{
				// ok
			}
			try
			{
				list.clear();
				assert false;
			} catch (UnsupportedOperationException e)
			{
				// ok
			}
		}
		
		{
			@Nonnull List<@Nonnull Integer> list = options.getIntList("twoInt");
			assert list.size() == 2;
			assert list.get(0) == 123;
			assert list.get(1) == 123456789;
		}
		
		{
			@Nonnull List<@Nonnull Integer> list = options.getIntList("withZero");
			assert list.size() == 2;
			assert list.get(0) == 1;
			assert list.get(1) == 0;
		}
		
		{
			@Nonnull List<@Nonnull Integer> list = options.getIntList("withNegative");
			assert list.size() == 2;
			assert list.get(0) == 5;
			assert list.get(1) == -7;
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Integer> list = options.getIntList("missing");
				assert false;
			} catch (MissingResourceException e)
			{
				assert "Missing option: missing".equals(e.getMessage()) : e.getMessage();
			}
		}
		
		{
			@Nonnull List<@Nonnull Integer> list = options.getIntList("defaultValue", "123,123456789");
			assert list.size() == 2;
			assert list.get(0) == 123;
			assert list.get(1) == 123456789;
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Integer> list = options.getIntList("letters");
				assert false;
			} catch (NumberFormatException e)
			{
				assert e.getMessage().contains("Problem with option [letters]: failed to parse [asd] as int in: [asd]") : e.getMessage();
			}
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Integer> list = options.getIntList("float");
				assert false;
			} catch (NumberFormatException e)
			{
				assert e.getMessage().contains("Problem with option [float]: failed to parse [1.34] as int in: [1.34]") : e.getMessage();
			}
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Integer> list = options.getIntList("emptyListItem");
				assert false;
			} catch (NumberFormatException e)
			{
				// Yes, there appears to be a bug in array formatting in Java?
				assert e.getMessage().contains("Problem with option [emptyListItem]: failed to parse [] as int in: [123, , 456]") : e.getMessage();
			}
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Integer> list = options.getIntList("empty", OptionConstraint.NON_EMPTY_COLLECTION);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Collection is empty in option: empty") : e.getMessage();
			}
		}
		
		{
			@Nonnull List<@Nonnull Integer> list = options.getIntList("twoInt", OptionConstraint.NON_EMPTY_COLLECTION, 
				OptionConstraint.POSITIVE, OptionConstraint.NON_NEGATIVE, OptionConstraint.NEGATIVE_ONE_OR_MORE);
			assert list.size() == 2;
			assert list.get(0) == 123;
			assert list.get(1) == 123456789;
		}
		
		{
			@Nonnull List<@Nonnull Integer> list = options.getIntList("singleZero", OptionConstraint.NON_EMPTY_COLLECTION, 
				OptionConstraint.NON_NEGATIVE, OptionConstraint.NEGATIVE_ONE_OR_MORE);
			assert list.size() == 1;
			assert list.get(0) == 0l;
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Integer> list = options.getIntList("singleZero", OptionConstraint.NON_EMPTY_COLLECTION, 
					OptionConstraint.POSITIVE, OptionConstraint.NON_NEGATIVE, OptionConstraint.NEGATIVE_ONE_OR_MORE);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Option [singleZero] value [0] violates constraint: POSITIVE") : e.getMessage();
			}
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Integer> list = options.getIntList("withZero", OptionConstraint.NON_EMPTY_COLLECTION, 
					OptionConstraint.POSITIVE);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Option [withZero] value [0] violates constraint: POSITIVE") : e.getMessage();
			}
		}
		
		{
			@Nonnull List<@Nonnull Integer> list = options.getIntList("withZero", OptionConstraint.NON_EMPTY_COLLECTION, 
				OptionConstraint.NON_NEGATIVE);
			assert list.size() == 2;
			assert list.get(0) == 1l;
			assert list.get(1) == 0l;
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Integer> list = options.getIntList("withMinusOne", OptionConstraint.NON_EMPTY_COLLECTION, 
					OptionConstraint.NON_NEGATIVE);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Option [withMinusOne] value [-1] violates constraint: NON_NEGATIVE") : e.getMessage();
			}
		}
		
		{
			@Nonnull List<@Nonnull Integer> list = options.getIntList("withMinusOne", OptionConstraint.NON_EMPTY_COLLECTION, 
				OptionConstraint.NEGATIVE_ONE_OR_MORE);
			assert list.size() == 2;
			assert list.get(0) == -1l;
			assert list.get(1) == 3l;
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull Integer> list = options.getIntList("withNegative", OptionConstraint.NON_EMPTY_COLLECTION, 
					OptionConstraint.NEGATIVE_ONE_OR_MORE);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Option [withNegative] value [-7] violates constraint: NEGATIVE_ONE_OR_MORE") : e.getMessage();
			}
		}
		
		{
			// Test unapplicable constraint + list
			try
			{
				options.getIntList("withNegative", OptionConstraint.NON_EMPTY_ELEMENT);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Option [withNegative] constraint type not applicable to number collection: NON_EMPTY_ELEMENT") : e.getMessage();
			}
		}
		
		{
			// Test unapplicable constraint + set
			try
			{
				options.getIntSet("withNegative", OptionConstraint.NON_EMPTY_ELEMENT);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Option [withNegative] constraint type not applicable to number collection: NON_EMPTY_ELEMENT") : e.getMessage();
			}
		}
		
		{
			// Test missing set without default option.
			try
			{
				options.getIntSet("missing");
				assert false;
			} catch (MissingResourceException e)
			{
				assert e.getMessage().contains("Missing option: missing") : e.getMessage();
			}
		}
		
		{
			// Test missing set with default option.
			@Nonnull Set<@Nonnull Integer> set = options.getIntSet("missing", "1,2");
			assert set.size() == 2 : set.size();
			assert set.contains(1);
			assert set.contains(2);
		}
		
		{
			// Test simple success scenario.
			@Nonnull Set<@Nonnull Integer> set = options.getIntSet("twoInt");
			assert set.size() == 2 : set.size();
			assert set.contains(123);
			assert set.contains(123456789);
		}
		
		{
			// Test duplicates
			try
			{
				options.getIntSet("duplicates");
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Duplicate value [123]: in option: duplicates") : e.getMessage();
			}
		}
	}	
	
	/**
	 * Test String lists & sets
	 */
	@Test
	public void testStringListAndSet()
	{
		BaseOptions options = new BaseOptions(Configuration.fromPropertiesFile("config/intList.properties"));
		
		{
			@Nonnull List<@Nonnull String> list = options.getStringList("empty");
			assert list.size() == 0;
		}
		
		{
			@Nonnull List<@Nonnull String> list = options.getStringList("oneInt");
			assert list.size() == 1;
			assert list.get(0).equals("123456789");
			
			// Test unmodifiability
			try
			{
				list.remove(0);
				assert false;
			} catch (UnsupportedOperationException e)
			{
				// ok
			}
			try
			{
				list.add("1");
				assert false;
			} catch (UnsupportedOperationException e)
			{
				// ok
			}
			try
			{
				list.clear();
				assert false;
			} catch (UnsupportedOperationException e)
			{
				// ok
			}
		}
		
		{
			@Nonnull List<@Nonnull String> list = options.getStringList("twoInt");
			assert list.size() == 2;
			assert list.get(0).equals("123");
			assert list.get(1).equals("123456789");
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull String> list = options.getStringList("missing");
				assert false;
			} catch (MissingResourceException e)
			{
				assert "Missing option: missing".equals(e.getMessage()) : e.getMessage();
			}
		}
		
		{
			@Nonnull List<@Nonnull String> list = options.getStringList("defaultValue", "123,123456789");
			assert list.size() == 2;
			assert list.get(0).equals("123");
			assert list.get(1).equals("123456789");
		}
		
		{
			@Nonnull List<@Nonnull String> list = options.getStringList("letters");
			assert list.size() == 1 : list;
			assert list.get(0).equals("asd") : list;
		}
		
		{
			@Nonnull List<@Nonnull String> list = options.getStringList("emptyListItem");
			assert list.size() == 3 : list;
			assert list.get(0).equals("123") : list;
			assert list.get(1).equals("") : list;
			assert list.get(2).equals("456") : list;
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull String> list = options.getStringList("emptyListItem", OptionConstraint.NON_EMPTY_ELEMENT);
				assert false;
			} catch (IllegalStateException e)
			{
				// Yes, there appears to be a bug in array formatting in Java?
				assert e.getMessage().contains("Option [emptyListItem] contains empty value: [123, , 456]") : e.getMessage();
			}
		}
		
		{
			try
			{
				@SuppressWarnings("unused")
				@Nonnull List<@Nonnull String> list = options.getStringList("empty", OptionConstraint.NON_EMPTY_COLLECTION);
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Collection is empty in option: empty") : e.getMessage();
			}
		}
		
		{
			@Nonnull List<@Nonnull String> list = options.getStringList("withZero", OptionConstraint.NON_EMPTY_COLLECTION);
			assert list.size() == 2;
			assert list.get(0).equals("1");
			assert list.get(1).equals("0");
		}
		
		{
			// Test unapplicable constraint + list
			for (@Nonnull OptionConstraint constraint : nnc(OptionConstraint.values()))
			{
				switch (constraint)
				{
					case NEGATIVE_ONE_OR_MORE:
					case NON_NEGATIVE:
					case POSITIVE:
						try
						{
							options.getStringList("withNegative", constraint);
							assert false;
						} catch (IllegalStateException e)
						{
							assert e.getMessage().contains("Option [withNegative] constraint type not applicable to string collection: " + constraint.name()) : e.getMessage();
						}
						break;
					case NON_EMPTY_COLLECTION:
					case NON_EMPTY_ELEMENT:
						break; // these are ok
				}
			}
		}
		
		{
			// Test unapplicable constraint + set
			for (@Nonnull OptionConstraint constraint : nnc(OptionConstraint.values()))
			{
				switch (constraint)
				{
					case NEGATIVE_ONE_OR_MORE:
					case NON_NEGATIVE:
					case POSITIVE:
						try
						{
							options.getStringSet("withNegative", constraint);
							assert false;
						} catch (IllegalStateException e)
						{
							assert e.getMessage().contains("Option [withNegative] constraint type not applicable to string collection: " + constraint.name()) : e.getMessage();
						}
						break;
					case NON_EMPTY_COLLECTION:
					case NON_EMPTY_ELEMENT:
						break; // these are ok
				}
			}
		}
		
		{
			// Test missing set without default option.
			try
			{
				options.getStringSet("missing");
				assert false;
			} catch (MissingResourceException e)
			{
				assert e.getMessage().contains("Missing option: missing") : e.getMessage();
			}
		}
		
		{
			// Test missing set with default option.
			@Nonnull Set<@Nonnull String> set = options.getStringSet("missing", "1,2");
			assert set.size() == 2 : set.size();
			assert set.contains("1");
			assert set.contains("2");
		}
		
		{
			// Test simple success scenario.
			@Nonnull Set<@Nonnull String> set = options.getStringSet("twoInt");
			assert set.size() == 2 : set.size();
			assert set.contains("123");
			assert set.contains("123456789");
		}
		
		{
			// Test duplicates
			try
			{
				options.getStringSet("duplicates");
				assert false;
			} catch (IllegalStateException e)
			{
				assert e.getMessage().contains("Duplicate value [123]: in option: duplicates") : e.getMessage();
			}
		}
	}	
	
	/**
	 * Tests non-negative values for ints and longs.
	 */
	@Test
	public void testNonNegative()
	{
		BaseOptions options = new BaseOptions(new MirrorFlatConfiguration("config/empty"));
		
		assert options.getIntNonNegative("0") == 0;
		assert options.getIntNonNegative("1") == 1;
		
		try
		{
			options.getIntNonNegative("-1");
			assert false;
		} catch (IllegalStateException e)
		{
			assert e.getMessage().contains("value must be a positive integer or zero") : e.getMessage();
		}
		try
		{
			options.getIntNonNegative("-2");
			assert false;
		} catch (IllegalStateException e)
		{
			assert e.getMessage().contains("value must be a positive integer or zero") : e.getMessage();
		}
		
		assert options.getLongNonNegative("0") == 0;
		assert options.getLongNonNegative("1") == 1;
		
		try
		{
			options.getLongNonNegative("-1");
			assert false;
		} catch (IllegalStateException e)
		{
			assert e.getMessage().contains("value must be a positive long or zero") : e.getMessage();
		}
		try
		{
			options.getLongNonNegative("-2");
			assert false;
		} catch (IllegalStateException e)
		{
			assert e.getMessage().contains("value must be a positive long or zero") : e.getMessage();
		}
		
		long bigLong = ((long)Integer.MAX_VALUE) + 1000000;
		
		assert options.getLongNonNegative("" + bigLong) == bigLong; // test that this is actually long, not int
		assert options.getLongNonNegative("" + bigLong, 123) == bigLong; // test that this is actually long, not int
		
		try
		{
			options.getLongNonNegative(MirrorFlatConfiguration.NULL_PREFIX);
			assert false;
		} catch (MissingResourceException e)
		{
			// expected
		}
		
		assert options.getLongNonNegative(MirrorFlatConfiguration.NULL_PREFIX, bigLong) == bigLong;
	}
}
