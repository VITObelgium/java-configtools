package be.vito.rma.configtools.common.api;

/**
 * @author (c) 2018 Stijn.VanLooy@vito.be
 *
 */
public interface ConfigurationService {

	public String getVersion ();

	public String getString (final String key);

	public long getLong (final String key);

	public int getInt (final String key);

	public double getDouble (final String key);

	public boolean getBoolean (final String key);

	public String getOptionalString (final String key);

	public Long getOptionalLong (final String key);

	public Integer getOptionalInt (final String key);

	public Double getOptionalDouble (final String key);

	public Boolean getOptionalBoolean (final String key);

}
