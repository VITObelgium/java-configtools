package be.vito.rma.configtools.common.services;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.vito.rma.configtools.common.api.ConfigurationFileService;
import be.vito.rma.configtools.common.api.ConfigurationService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import lombok.extern.slf4j.Slf4j;

/**
 * @author (c) 2016-2018 Stijn.VanLooy@vito.be
 *
 *  "default" (optional) configuration parameters:
 * 		loglevel : reset root log level to the given level
 * 			valid values: all, trace, debug, info, warn, error, off
 * 		logfile : log to the given file with daily rotation
 * 			for example: /var/log/marvin/myapp.log
 * 				this will log to /var/log/marvin/myapp.log.YYYY-MM-DD
 * 		logpattern: logging pattern to user for the log file
 * 			required when using the logfile configuration parameter
 * 			for example: %d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n
 * 		version: version number reported by the version endpoint
 * 			this one will always be fetched from the default configuration
 * 			(as returned by {@link ConfigurationFileService#getDefaultResourceName()},
 * 			if present in the configuration file, it will be ignored)
 * 			(if not found in the default configuration, this will result in internal server error)
 */
@Slf4j
public class DefaultConfigurationService  implements ConfigurationService {

	private ResourceBundle config, defaultConfig;
	private Properties overriddenParameters = new Properties();

	public final static String KEY_LOGLEVEL = "loglevel";
	public final static String KEY_LOGFILE = "logfile";
	public final static String KEY_LOGPATTERN = "logpattern";
	public final static String KEY_VERSION = "version";

	public DefaultConfigurationService(final ConfigurationFileService configFileService) {

		// load resource bundles
		try {
			defaultConfig = ResourceBundle.getBundle(configFileService.getDefaultResourceName());
		} catch (MissingResourceException | NullPointerException e) {
			log.warn("no default configuration resource file available: calls to getVersion() will fail", e);
			defaultConfig = null;
		}
		File configFile = loadConfigurationFile(configFileService.getConfigFile());
		if (configFile != null && configFile.exists()) {
			String fileName = configFile.getName();
			log.debug("loading configuration from " + fileName);
			String prefix = fileName.substring(0, fileName.length() - ".properties".length());
			URLClassLoader urlLoader;
			try {
				urlLoader = new URLClassLoader(new java.net.URL[] {configFile.getParentFile().toURI().toURL()}, null);
				config = ResourceBundle.getBundle(prefix, Locale.getDefault(), urlLoader);
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		} else {
			if (defaultConfig == null) {
				String message = "no configuration file available and no default configuration resource file available";
				RuntimeException e = new RuntimeException(message);
				log.error(message, e);
				throw e;
			} else {
				log.warn((configFile == null ? "no configuration file available: "
						: configFile.getAbsolutePath() + " not found: ") + "using default configuration");
				config = defaultConfig;
			}
		}

		// reset root log level if required
		setLoglevel();

		// configure log file if required
		configureLogFile();

		// warn if version is defined in non-default config
		if (config != defaultConfig && getOptionalString(KEY_VERSION, config) != null)
			log.warn("'" + KEY_VERSION + "' is defined in " + configFileService.getConfigFile().getAbsolutePath()
					+ " It's value will be ignored");

	}

	private File loadConfigurationFile (final File configFile) {
		if (configFile == null || !configFile.exists()) return null;
		String fileName = configFile.getName();
		if (fileName.endsWith(".properties")) return configFile;
		// for other extensions: copy to temp file that does end with .properties
		try {
			File file = Files.createTempFile("configtools", ".properties").toFile();
			Files.copy(configFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			file.deleteOnExit();
			return file;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void setLoglevel () {
		String loglevel = getOptionalString(KEY_LOGLEVEL, config);
		// if not defined: do nothing => keep default root log level
		if (loglevel != null) {
			ch.qos.logback.classic.Logger rootLogger =
				(ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
			rootLogger.setLevel(Level.valueOf(loglevel));
		}
	}

	private void configureLogFile () {
		String logfile = getOptionalString (KEY_LOGFILE, config);
		String logpattern = getOptionalString(KEY_LOGPATTERN, config);

		if (logfile != null && logpattern != null) {

		    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		    RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<>();
		    rollingFileAppender.setAppend(true);
		    rollingFileAppender.setContext(context);

		    TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
		    rollingPolicy.setFileNamePattern(logfile + ".%d{yyyy-MM-dd}");
		    rollingPolicy.setMaxHistory(Integer.MAX_VALUE);
		    rollingPolicy.setParent(rollingFileAppender);
		    rollingPolicy.setContext(context);
		    rollingPolicy.start();

		    rollingFileAppender.setRollingPolicy(rollingPolicy);

		    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		    encoder.setPattern(logpattern);
		    encoder.setContext(context);
		    encoder.start();

		    rollingFileAppender.setEncoder(encoder);
		    rollingFileAppender.start();

		    ch.qos.logback.classic.Logger rootLogger =
					(ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

		    // remove existing appenders and replace by our appender
 			rootLogger.detachAndStopAllAppenders();
		    // and add the newly created appender to the root logger;
		    rootLogger.addAppender(rollingFileAppender);
		}
	}

	/**
	 * Override a parameter in the configuration. Use with care: can cause counterintuitive behavior
	 * @param key
	 * @param value
	 */
	public void overrideParameter (final String key, final String value) {
		if (key.equalsIgnoreCase(KEY_VERSION))
			throw new RuntimeException("Cannot override " + KEY_VERSION + " parameter");
		overriddenParameters.setProperty(key, value);
		if (key.equals(KEY_LOGLEVEL))
			setLoglevel();
		else if (key.equals(KEY_LOGFILE) || key.equals(KEY_LOGPATTERN))
			configureLogFile();
	}

	private String getOptionalString (final String key, final ResourceBundle bundle) {
		try {
			// use overridden value if present
			String out = overriddenParameters.getProperty(key);
			if (out == null)
				out = bundle.getString(key);
			out = out.trim();
			if (out.length() == 0) return null;	// empty value == no value
			return out;
		} catch (Exception e) {
			return null;
		}
	}

	private String getString (final String key, final ResourceBundle bundle) {
		String out = getOptionalString(key, bundle);
		if (out == null) {
			String message = "required configuration parameter " + key + " not found";
			log.error(message);
			throw new RuntimeException(message);
		}
		return out;
	}

	@Override
	public String getVersion () {
		return getString(KEY_VERSION, defaultConfig);
	}

	@Override
	public String getString (final String key) {
		return getString(key, config);
	}

	@Override
	public long getLong (final String key) {
		return Long.parseLong(getString(key));
	}

	@Override
	public int getInt (final String key) {
		return Integer.parseInt(getString(key));
	}

	@Override
	public double getDouble (final String key) {
		return Double.parseDouble(getString(key));
	}

	@Override
	public boolean getBoolean (final String key) {
		return getString(key).equalsIgnoreCase("true");
	}

	@Override
	public String getOptionalString (final String key) {
		return getOptionalString(key, config);
	}

	@Override
	public Long getOptionalLong (final String key) {
		String data = getOptionalString(key);
		if (data == null) return null;
		return Long.parseLong(data);
	}

	@Override
	public Integer getOptionalInt (final String key) {
		String data = getOptionalString(key);
		if (data == null) return null;
		return Integer.parseInt(data);
	}

	@Override
	public Double getOptionalDouble (final String key) {
		String data = getOptionalString(key);
		if (data == null) return null;
		return Double.parseDouble(data);
	}

	@Override
	public Boolean getOptionalBoolean (final String key) {
		String data = getOptionalString(key);
		if (data == null) return null;
		return data.equalsIgnoreCase("true");
	}

}
