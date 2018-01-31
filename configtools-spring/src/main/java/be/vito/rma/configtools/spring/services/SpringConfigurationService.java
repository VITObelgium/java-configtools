package be.vito.rma.configtools.spring.services;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import be.vito.rma.configtools.common.api.ConfigurationFileService;
import be.vito.rma.configtools.common.api.ConfigurationService;
import be.vito.rma.configtools.common.services.DefaultConfigurationService;

/**
 * @author (c) 2018 Stijn.VanLooy@vito.be
 *
 *	requires a Spring bean implementing the ConfigurationFileService interface
 *
 *	"default" (optional) configuration parameters:
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
 */
@Component
public class SpringConfigurationService implements ConfigurationService {


	@Autowired private ConfigurationFileService configFileService;
	private DefaultConfigurationService service;

	@PostConstruct
	public void init () {
		service = new DefaultConfigurationService(configFileService);
	}

	/**
	 * Override a parameter in the configuration. Use with care: can cause counterintuitive behavior
	 * @param key
	 * @param value
	 */
	public void overrideParameter (final String key, final String value) {
		service.overrideParameter(key, value);
	}

	@Override
	public String getVersion() {
		return service.getVersion();
	}

	@Override
	public String getString(final String key) {
		return service.getString(key);
	}

	@Override
	public long getLong(final String key) {
		return service.getLong(key);
	}

	@Override
	public int getInt(final String key) {
		return service.getInt(key);
	}

	@Override
	public double getDouble(final String key) {
		return service.getDouble(key);
	}

	@Override
	public boolean getBoolean(final String key) {
		return service.getBoolean(key);
	}

	@Override
	public String getOptionalString(final String key) {
		return service.getOptionalString(key);
	}

	@Override
	public Long getOptionalLong(final String key) {
		return service.getOptionalLong(key);
	}

	@Override
	public Integer getOptionalInt(final String key) {
		return service.getOptionalInt(key);
	}

	@Override
	public Double getOptionalDouble(final String key) {
		return service.getOptionalDouble(key);
	}

	@Override
	public Boolean getOptionalBoolean(final String key) {
		return service.getOptionalBoolean(key);
	}

}
