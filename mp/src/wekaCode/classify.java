package wekaCode;

import weka.classifiers.Evaluation;
import weka.classifiers.functions.Logistic;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public class classify {

	Logistic tree=null;
	Instances data=null;
	public void train(String file) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(file));
		data = new Instances(reader);
		reader.close();
		data.setClassIndex(data.numAttributes() - 1);
		String[] options = new String[1];
		options[0] = "-U";
		tree = new Logistic();         
		tree.setOptions(options);     
		tree.buildClassifier(data);
	}

	public static void main(String[] args)
	{
		classify c=new classify();
		try {
			c.train("dtree.arff");
			double[] ex={0,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,3,0,5,0,0,0,0,0,0,0,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
			for(double d : c.get_probability(ex))
			{
				System.out.print(d+" , ");;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public double[] get_probability(double[] instance_data) throws Exception
	{
		Instance ex = new Instance(instance_data.length+1); 
		int c=0;
		for(double d : instance_data)
		{
			ex.setValue(data.attribute(c), d);
			c++;
		}
		ex.setMissing(ex.numAttributes()-1);
		return tree.distributionForInstance(ex);
	}
	
}
