package com.wynter.pillvox.domain;

/** What the vision stage reported about the pill in front of the camera. */
public final class Observed {
    public final String imprint, shape, colour;
    public Observed(String imprint, String shape, String colour) {
        this.imprint = imprint; this.shape = shape; this.colour = colour;
    }
}
