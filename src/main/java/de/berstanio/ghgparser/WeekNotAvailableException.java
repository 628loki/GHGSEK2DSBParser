package de.berstanio.ghgparser;

public class WeekNotAvailableException extends DSBNotLoadableException{
	public WeekNotAvailableException (Throwable cause) {
		super(cause);
	}

	public WeekNotAvailableException (String cause) {
		super(cause);
	}
}
