package com.bsycorp.kees.data;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class BaseConverter {

    public static final float BITS_IN_BYTE = 8.0f;
    private String charSet;
    private int base = -1;
    public BaseConverter(String charSetAsString) {
        assert((charSetAsString != null) && (charSetAsString.length() > 1));
        this.charSet = charSetAsString;
    }

    //This can return a variable number of characters, depending on the generated bytes, if you want a fixed length reuslt, add desiredLength.
    public String encode(byte[] b) {

        //don't pass NULL and has something in it

        assert((b != null) && (b.length > 0));
        //Calculate the divider based on the size of the character set (e.g 62 characters === base 62)

        BigInteger divider = BigInteger.valueOf(charSet.length());

        //create a +ive BigInteger from the byte array

        BigInteger currentDividend = new BigInteger(1, b);

        //The StringBuilder will hold the ever increasing string representation

        StringBuilder sb = new StringBuilder();
        do {

            //Get the quotient and remainder when dividing the current dividend
            BigInteger[] bigIntDivideAndRemainder = currentDividend.divideAndRemainder(divider);

            //Find character in the charSet at the position of the remainder and insert at start of builder
            sb.insert(0, charSet.charAt(bigIntDivideAndRemainder[1].intValue()));

            //Set the current dividend to the quotient
            currentDividend = bigIntDivideAndRemainder[0];

        } while (currentDividend.compareTo(BigInteger.ZERO) >  0);
        //The final string is returned
        return sb.toString();

    }

    public String encode(byte[] b, int desiredLength) {
        String converted = encode(b);
        while(converted.length() < desiredLength){
            //left pad if we haven't hit our
            converted = "0" + converted;
        }
        return converted;
    }

    public byte[] decode(String baseAsStr) {
        //Initialise
        BigInteger runningValue = new BigInteger("0");

        //Reverse the string so earliest (lowest) digits come first
        StringBuilder sb = (new StringBuilder(baseAsStr)).reverse();

        //The multiplier will go up in multiple of the increment
        BigInteger multiplier = BigInteger.ONE;
        BigInteger increment = BigInteger.valueOf(charSet.length());

        for (int i=0; i < sb.length(); i++) {

            //Get the index position of the current character in the character set
            BigInteger value = BigInteger.valueOf(charSet.indexOf(sb.charAt(i)));
            //and multiply
            runningValue = runningValue.add(multiplier.multiply(value));

            //increase the multiplier
            multiplier = multiplier.multiply(increment);
        }

        //During decode we want to return the unsigned bytes for a given input, by default toByteArray() returns the signed values, meaning sometimes
        //it has a leading 0x00 byte, we can detect this by comparing the bitLength (the number of bits for a given input) to the actual size toByteArray()
        //gives us, most of the time they are they same, but when we get the leading 0x00 byte it is larger than the bitLength.
        //When it is larger than the bitLength trim off the leading byte and return the rest.
        double bitLengthBytes = Math.ceil(runningValue.bitLength() / BITS_IN_BYTE);
        byte[] resultBytes = runningValue.toByteArray();
        if(bitLengthBytes < resultBytes.length){
            return Arrays.copyOfRange(resultBytes, 1, resultBytes.length);
        }
        return resultBytes;
    }

    public byte[] decode(String baseAsStr, int desiredLength) {
        byte[] result = decode(baseAsStr);
        if(result.length > desiredLength){
            throw new RuntimeException("Decoded length of " + result.length + " is longer than desired length of " + desiredLength);
        }

        //then add in the proper bytes after the padding.
        ByteBuffer paddedResult = ByteBuffer.allocate(desiredLength);
        paddedResult.position(desiredLength - result.length);
        paddedResult.put(decode(baseAsStr));
        return paddedResult.array();
    }

}
