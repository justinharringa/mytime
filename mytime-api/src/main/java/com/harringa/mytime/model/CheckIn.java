package com.harringa.mytime.model;

import java.util.Date;

public class CheckIn implements Checkpoint {

    private final Date date;

    public CheckIn(final Date date) {
        this.date = date;
    }

    @Override
    public Date getDate() {
        return date;
    }
}
