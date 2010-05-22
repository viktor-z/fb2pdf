package com.fb2pdf.hadoop;

import java.util.UUID;

import org.apache.mahout.utils.vectors.text.DictionaryVectorizer;


public class DictionaryVectorizerDriver{

	/**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        if(args.length != 2)
        {
            System.err.println("Usage DictionaryVectorizerDriver <src> <dst>");
            System.exit(1);
        } else
        	DictionaryVectorizer.createTermFrequencyVectors(args[0], args[1], 2, 1,
        	        1.0F, 1, 100, false);
    }
	
}
