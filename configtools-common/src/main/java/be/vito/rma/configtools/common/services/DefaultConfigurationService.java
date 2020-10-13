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
 * @author (c) 2016-2020 Stijn.VanLooy@vito.be
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
 *
 *  if the configuration file is set and does exist, the configuration file is used.
 *  else if the configuration file is not set or does not exist
 *  if the environment variable CONFIG_USE_DEFAULT has a value (does not matter which value, for example "yes")
 *  then the default configuration is used
 *  else (CONFIG_USE_DEFAULT is not set) the configuration is read from the environment variables.
 *  configuration keys are transformed into environment variable names as follows:
 *  	all chars uppercased, and dots replaced by underscores
 *   for example: the configuration key "an.example" results in env. var. name "AN_EXAMPLE"
 */
@Slf4j
public class DefaultConfigurationService  implements ConfigurationService {

	private ResourceBundle config, defaultConfig;
	private final Properties overriddenParameters = new Properties();

	public final static String KEY_LOGLEVEL = "loglevel";
	public final static String KEY_LOGFILE = "logfile";
	public final static String KEY_LOGPATTERN = "logpattern";
	public final static String KEY_VERSION = "version";
	public final static String ENV_USE_DEFAULT = "CONFIG_USE_DEFAULT";

	public DefaultConfigurationService(final ConfigurationFileService configFileService) {

		// load resource bundles
		try {
			defaultConfig = ResourceBundle.getBundle(configFileService.getDefaultResourceName());
		} catch (MissingResourceException | NullPointerException e) {
			final String message = "failed to load default configuration";
			final RuntimeException ex = new RuntimeException(message);
			log.error(message, ex);
			throw ex;
		}
		final File configFile = loadConfigurationFile(configFileService.getConfigFile());
		if (configFile != null && configFile.exists()) {
			final String fileName = configFile.getName();
			log.debug("loading configuration from " + fileName);
			final String prefix = fileName.substring(0, fileName.length() - ".properties".length());
			URLClassLoader urlLoader;
			try {
				urlLoader = new URLClassLoader(new java.net.URL[] {configFile.getParentFile().toURI().toURL()}, null);
				config = ResourceBundle.getBundle(prefix, Locale.getDefault(), urlLoader);
			} catch (final MalformedURLException e) {
				throw new RuntimeException(e);
			}
		} else {
			if (System.getenv().get(ENV_USE_DEFAULT) != null) {
				log.debug("using default configuration");
				config = defaultConfig;
			} else {
				log.debug("loading configuration from environment variables");
				config = null;
			}
		}

		// reset root log level if required
		setLoglevel();

		// configure log file if required
		configureLogFile();

		// warn if version is defined in non-default config
		if (config != null && config != defaultConfig && getOptionalString(KEY_VERSION, config) != null)
			log.warn("'" + KEY_VERSION + "' is defined in " + configFileService.getConfigFile().getAbsolutePath()
					+ " It's value will be ignored");

	}

	private File loadConfigurationFile (final File configFile) {
		if (configFile == null || !configFile.exists()) return null;
		final String fileName = configFile.getName();
		if (fileName.endsWith(".properties")) return configFile;
		// for other extensions: copy to temp file that does end with .properties
		try {
			final File file = Files.createTempFile("configtools", ".properties").toFile();
			Files.copy(configFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			file.deleteOnExit();
			return file;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void setLoglevel () {
		final String loglevel = getOptionalString(KEY_LOGLEVEL, config);
		// if not defined: do nothing => keep default root log level
		if (loglevel != null) {
			final ch.qos.logback.classic.Logger rootLogger =
				(ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
			rootLogger.setLevel(Level.valueOf(loglevel));
		}
	}

	private void configureLogFile () {
		final String logfile = getOptionalString (KEY_LOGFILE, config);
		final String logpattern = getOptionalString(KEY_LOGPATTERN, config);

		if (logfile != null && logpattern != null) {

		    final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		    final RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<>();
		    rollingFileAppender.setAppend(true);
		    rollingFileAppender.setContext(context);

		    final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
		    rollingPolicy.setFileNamePattern(logfile + ".%d{yyyy-MM-dd}");
		    rollingPolicy.setMaxHistory(Integer.MAX_VALUE);
		    rollingPolicy.setParent(rollingFileAppender);
		    rollingPolicy.setContext(context);
		    rollingPolicy.start();

		    rollingFileAppender.setRollingPolicy(rollingPolicy);

		    final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		    encoder.setPattern(logpattern);
		    encoder.setContext(context);
		    encoder.start();

		    rollingFileAppender.setEncoder(encoder);
		    rollingFileAppender.start();

		    final ch.qos.logback.classic.Logger rootLogger =
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
			if (out == null) {
				if (bundle != null) {
					// read from given config bundle
					out = bundle.getString(key);
				} else {
					// read from environment variables
					final String environmentVariableName = key.toUpperCase().replace(".", "_");
					out = System.getenv().get(environmentVariableName);
				}
			}
			if (out == null) return null;	// no value found
			out = out.strip();
			if (out.length() == 0) return null;	// empty value == no value
			return out;
		} catch (final Exception e) {
			return null;
		}
	}

	private String getString (final String key, final ResourceBundle bundle) {
		final String out = getOptionalString(key, bundle);
		if (out == null) {
			// FIXME: key omzetten naar HOOFDLETTERS en _ indien we met env. vars werken
			final String message = "required configuration parameter " + key + " not found";
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
		final String data = getOptionalString(key);
		if (data == null) return null;
		return Long.parseLong(data);
	}

	@Override
	public Integer getOptionalInt (final String key) {
		final String data = getOptionalString(key);
		if (data == null) return null;
		return Integer.parseInt(data);
	}

	@Override
	public Double getOptionalDouble (final String key) {
		final String data = getOptionalString(key);
		if (data == null) return null;
		return Double.parseDouble(data);
	}

	@Override
	public Boolean getOptionalBoolean (final String key) {
		final String data = getOptionalString(key);
		if (data == null) return null;
		return data.equalsIgnoreCase("true");
	}

}
