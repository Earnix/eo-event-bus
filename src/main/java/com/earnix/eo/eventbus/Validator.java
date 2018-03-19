package com.earnix.eo.eventbus;

class Validator {
    
    static void notNull(Object argument) {
        if (argument == null) {
            throw new NullPointerException("Must be not null");
        }
    }
    
    static void notNull(Object argument, String message) {
        if (argument == null) {
            throw new NullPointerException(message);
        }
    }

    static void isTrue(boolean argument) {
        if(!argument){
            throw new IllegalArgumentException("Must be true");
        }
    }
    
    static void isTrue(boolean argument, String message) {
        if(!argument){
            throw new IllegalArgumentException(message);
        }
    }
}
