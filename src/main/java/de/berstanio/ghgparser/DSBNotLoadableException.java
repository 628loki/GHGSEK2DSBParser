package de.berstanio.ghgparser;

/**
 * Eigene Exception zur leichteren Fehlerbehandlung
 */
public class DSBNotLoadableException extends Exception{

    private static final long serialVersionUID = 672631111598658084L;

    public DSBNotLoadableException(Throwable cause) {
        super(cause);
    }

    public DSBNotLoadableException(String cause) {
        super(cause);
    }
}
