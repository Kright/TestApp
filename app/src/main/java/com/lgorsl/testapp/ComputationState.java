package com.lgorsl.testapp;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;

/**
 * Created by lgor on 13.06.2016.
 */
public class ComputationState implements Serializable{

    private static final long serialVersionUID = 1423223L;

    public final BigInteger[] values;

    public ComputationState(ArrayList<BigInteger> valuesList) {
        this.values = valuesList.toArray(new BigInteger[valuesList.size()]);
    }

    public ComputationState(){
        values = new BigInteger[1];
        values[0] = new BigInteger("1");
    }
}
