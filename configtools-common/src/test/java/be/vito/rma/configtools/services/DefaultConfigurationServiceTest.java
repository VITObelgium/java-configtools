package be.vito.rma.configtools.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import be.vito.rma.configtools.common.api.ConfigurationFileService;
import be.vito.rma.configtools.common.services.DefaultConfigurationService;
import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;

/**
 * @author (c) 2018 Stijn.VanLooy@vito.be
 *
 */
@Slf4j
public class DefaultConfigurationServiceTest {

	private DefaultConfigurationService config;

	@Before
	public void init () {


		config = new DefaultConfigurationService(new ConfigurationFileService() {
			@Override
			public String getDefaultResourceName() {
				return "test_default_configuration";
			}

			@Override
			public File getConfigFile() {
				return new File("src/test/resources/file/configurationfile.cfg");
			}
		});
	}

	@Test
	public void typeTest () {

		String s = config.getString("value.string");
		long l = config.getLong("value.long");
		int i = config.getInt("value.int");
		double d = config.getDouble("value.double");
		boolean b = config.getBoolean("value.boolean");

		Assert.assertEquals("This is a test", s);
		Assert.assertEquals(123456L, l);
		Assert.assertEquals(123, i);
		Assert.assertEquals(1.235894, d, Double.MIN_VALUE);
		Assert.assertTrue(b);
	}

	@Test
	public void notInFileTest () {

		// don't need to log the error message we will be expecting
		config.overrideParameter("loglevel", "off");

		try {
			config.getString("not.in.file");
			Assert.fail("requesting configuration parameter not.in.file should fail");
		} catch (RuntimeException e) {
			Assert.assertEquals("required configuration parameter not.in.file not found", e.getMessage());
		}

	}

	@Test
	public void versionTest() {

		String actualVersion = config.getVersion();
		String ignoredVersion = config.getString("version");
		Assert.assertNotEquals(ignoredVersion, actualVersion);

	}

	@Test
	public void overrideTest () {

		config.overrideParameter("value.string", "foo bar");

		Assert.assertEquals("foo bar", config.getString("value.string"));

	}

	private File getTempFile () {
		File out;
		try {
			out = Files.createTempFile("configtools-test-", ".log").toFile();
			out.deleteOnExit();
			return out;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private int countLines (final File file) {
		try {
			return Files.readAllLines(file.toPath()).size();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void logFileTest () throws IOException {
		File configFile = getTempFile();

		config.overrideParameter(DefaultConfigurationService.KEY_LOGLEVEL, Level.WARN.toString());
		config.overrideParameter(DefaultConfigurationService.KEY_LOGFILE, configFile.getAbsolutePath());
		config.overrideParameter(DefaultConfigurationService.KEY_LOGPATTERN, "%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n");

		Assert.assertEquals(0, countLines(configFile));

		log.info("should not be visible");
		Assert.assertEquals(0, countLines(configFile));

		// TODO: cannot test: file is not yet flushed to the file system
//		log.error("should be visible");
//		Assert.assertEquals(1, countLines(configFile));

	}
}
