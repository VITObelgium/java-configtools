package be.vito.rma.configtools.common.api;

import java.io.File;

/**
 * @author (c) 2016 Stijn.VanLooy@vito.be
 *
 */
public interface ConfigurationFileService {

	/**
	 * The configuration file to use.
	 * note: the configuration file must have the extension ".properties"
	 * for example: "/etc/marvin/myapp.properties"
	 * @return
	 */
	public File getConfigFile();

	/**
	 * The name of the resource in the classpath which is used as
	 * a fallback if the configuration file on the file system cannot be found.
	 * note: do not include the ".properties" extension, only the resource name must be returned
	 * for example: "defaultConfig", which will load "defaultConfig.properties" in the classpath
	 * @return
	 */
	public String getDefaultResourceName();

}
