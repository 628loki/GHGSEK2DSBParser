package de.berstanio.ghgparser;

/**
 * Eigene Exception zur leichteren Fehlerbehandlung
 */
public class DSBNotLoadableException extends Exception{

    public DSBNotLoadableException(Throwable cause) {
        super(cause);
    }
}
