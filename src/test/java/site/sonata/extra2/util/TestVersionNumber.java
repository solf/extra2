/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.util;

import static org.testng.Assert.assertEquals;
import static site.sonata.extra2.util.VersionNumber.compare;

import javax.annotation.ParametersAreNonnullByDefault;

import org.testng.annotations.Test;

import site.sonata.extra2.util.VersionNumber;

/**
 * Tests for {@link VersionNumber}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class TestVersionNumber
{
	@Test
	public void testCompare()
	{
		assert -1 == compare("1.0.1").with("1.0.2");
		assert 1 == compare("1.0.2.1").with("1.0.2");
		assert compare("1.0.2").eq("1.0.2");
		
		{
			VersionNumber v = new VersionNumber("1.3.37");
			assert v.gt("1.2.999");
			assert !v.lt("1.2.999");
			assert v.ge("1.2.999");
			assert !v.le("1.2.999");
			assert v.eq("1.3.37.0");
		}
		
		{
			VersionNumber v = new VersionNumber("1.3.37");
			assert !v.gt("1.11.1");
			assert v.lt("1.11.1");
			assert !v.ge("1.11.1");
			assert v.le("1.11.1");
		}
		
		{
			VersionNumber v = new VersionNumber("2.0.0");
			assert v.eq("2.0");
			assert !v.gt("2.0");
			assert !v.lt("2.0");
			assert v.eq("2.0.0");
			assert v.ge("2.0.0");
			assert v.le("2.0.0");
			assert v.eq("2");
			assert !v.gt("2");
			assert !v.lt("2");
			
			assert v.gt("1.0");
			assert v.gt("1.9");
			assert v.gt("1.9.9");
			assert v.gt("1.9.9.9");
			
			assert v.lt("3.0");
			assert v.lt("2.1");
			assert v.lt("2.0.1");
			assert v.lt("2.0.0.1");
		}
		
		{
			VersionNumber v = new VersionNumber("2");
			assert v.eq("2.0");
			assert !v.gt("2.0");
			assert !v.lt("2.0");
			assert v.eq("2.0.0");
			assert v.ge("2.0.0");
			assert v.le("2.0.0");
			assert v.eq("2");
			assert !v.gt("2");
			assert !v.lt("2");
			
			assert v.gt("1.0");
			assert v.gt("1.9");
			assert v.gt("1.9.9");
			assert v.gt("1.9.9.9");
			
			assert v.lt("3.0");
			assert v.lt("2.1");
			assert v.lt("2.0.1");
			assert v.lt("2.0.0.1");
		}
	}
	
	@Test
	public void testToString()
	{
		assertEquals(new VersionNumber("3.0").toString(), "3.0"); 
		assertEquals(new VersionNumber("3").toString(), "3"); 
		assertEquals(new VersionNumber("3.0.0").toString(), "3.0.0"); 
		assertEquals(new VersionNumber("3.0.0-SNAPSHOT").toString(), "3.0.0-SNAPSHOT"); 
	}

	@Test
	public void testApproximation()
	{
		assert compare("1.0.2").agt("1.0"); // ~> 1.0 => >= 1.0 && < 2.0
		assert !compare("2.0").agt("1.0");
		assert compare("1.9").agt("1.0"); // ~> 1.0 => >= 1.0 && < 2.0
		assert !compare("0.9").agt("1.0");
		assert compare("1.0.2").agt("1.0.2"); // ~> 1.0.2 => >= 1.0.2 && < 1.1
		assert !compare("1.2").agt("1.0.2");
	}
}
