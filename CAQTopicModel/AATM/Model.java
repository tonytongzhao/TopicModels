package AATM;

import AATM.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

public class Model {

	// ---------------------------------------------------------------
	// Class Variables
	// ---------------------------------------------------------------

	public static final boolean use_tfidf = true;

	public static String tassignSuffix; // suffix for topic assignment file

	public static String thetaSuffix; // suffix for theta (topic - document

	// distribution) file

	public static String phiSuffix; // suffix for phi file (topic - word

	// distribution) file

	public static String othersSuffix; // suffix for containing other

	// parameters

	public static String twordsSuffix; // suffix for file containing

	// words-per-topics

	public static String piSuffix;

	// ---------------------------------------------------------------
	// Model Parameters and Variables
	// ---------------------------------------------------------------

	public String wordMapFile; // file that contain word to id map

	public String trainlogFile; // training log file

	public String dir;

	public String dfile;

	public String modelName;

	public int modelStatus; // see Constants class for status of model

	public LDADataset data; // link to a dataset

	public int M; // dataset size (i.e., number of docs)

	public int A; // number of authors

	public int V; // vocabulary size

	public int K; // number of topics

	public double alpha, beta, gamma; // LDA hyperparameters

	public int niters; // number of Gibbs sampling iteration

	public int liter; // the iteration at which the model was saved

	public int savestep; // saving period

	public int twords; // print out top words per each topic

	public int withrawdata;

	// Estimated/Inferenced parameters
	public double[][][] theta; // theta: author - topic distributions, size A x

	// A x K

	public double[][] phi; // phi: topic-word distributions, size K x V

	public double[][] pi; // pi: asker - answerer distributions, size A x A

	public double[][] gammaforusers;

	public int[][] ARcount; // background matrix, size A x A

	// Weighting values
	public double[][] word_weights; // maps each word/doc pair to a weight, size

	// V x M

	// Temp variables while sampling
	public Vector<Integer>[] z; // topic assignments for words, size M x

	// doc.size()

	public Vector<Integer>[] za; // author assignments for words, size M x
	
	public Vector<Integer> askers;
	// doc.size()

	public Vector<Integer>[] zr;

	public Vector<Integer>[] dr;

	public Vector<Integer>[] da; // author assignments for documents

	protected int[][] nw; // nw[i][j]: number of instances of word/term i

	// assigned to topic j, size V x K

	protected int[][][] naat;

	protected int[][] naa;

	protected int[][] naatsum;

	protected int[] naasum;

	protected int[] nwsum;

	// temp variables for sampling
	protected double[] p;

	// ---------------------------------------------------------------
	// Constructors
	// ---------------------------------------------------------------

	public Model() {
		setDefaultValues();
	}

	/**
	 * Set default values for variables
	 */
	public void setDefaultValues() {
		wordMapFile = "wordmap.txt";
		trainlogFile = "trainlog.txt";
		tassignSuffix = ".tassign";
		thetaSuffix = ".theta";
		phiSuffix = ".phi";
		piSuffix = ".pi";
		othersSuffix = ".others";
		twordsSuffix = ".twords";

		dir = "./";
		dfile = "trndocs.dat";
		modelName = "model-final";
		modelStatus = Constants.MODEL_STATUS_UNKNOWN;

		M = 0;
		V = 0;
		A = 0;
		K = 100;
		alpha = 50.0 / K;
		beta = 0.1;
		gamma = 0.01;
		niters = 2000;
		liter = 0;

		gammaforusers = null;
		ARcount = null;
		askers=null;
		z = null;
		da = null;
		za = null;
		zr = null;
		dr = null;
		nw = null;
		naa = null;
		naat = null;
		naasum = null;
		nwsum = null;
		naatsum = null;
		theta = null;
		phi = null;
		pi = null;

	}

	// ---------------------------------------------------------------
	// I/O Methods
	// ---------------------------------------------------------------
	/**
	 * read other file to get parameters
	 */

	// ***
	// DON'T USE - HAVE NOT UPDATED THIS METHOD FOR ATM
	// ***
	protected boolean readOthersFile(String otherFile) {
		// open file <model>.others to read:

		try {
			BufferedReader reader = new BufferedReader(
					new FileReader(otherFile));
			String line;
			while ((line = reader.readLine()) != null) {
				StringTokenizer tknr = new StringTokenizer(line, "= \t\r\n");

				int count = tknr.countTokens();
				if (count != 2)
					continue;

				String optstr = tknr.nextToken();
				String optval = tknr.nextToken();

				if (optstr.equalsIgnoreCase("alpha")) {
					alpha = Double.parseDouble(optval);
				} else if (optstr.equalsIgnoreCase("beta")) {
					beta = Double.parseDouble(optval);
				} else if (optstr.equalsIgnoreCase("gamma")) {
					gamma = Double.parseDouble(optval);
				} else if (optstr.equalsIgnoreCase("ntopics")) {
					K = Integer.parseInt(optval);
				} else if (optstr.equalsIgnoreCase("liter")) {
					liter = Integer.parseInt(optval);
				} else if (optstr.equalsIgnoreCase("nwords")) {
					V = Integer.parseInt(optval);
				} else if (optstr.equalsIgnoreCase("ndocs")) {
					M = Integer.parseInt(optval);
				} else {
					// any more?
				}
			}

			reader.close();
		} catch (Exception e) {
			System.out.println("Error while reading other file:"
					+ e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	// ***
	// DON'T USE - HAVE NOT UPDATED THIS METHOD FOR ATM
	// ***

	protected boolean readTAssignFile(String tassignFile) {
		try {
			int i, j;
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(tassignFile), "UTF-8"));

			String line;
			z = new Vector[M];
			za = new Vector[M];
			zr = new Vector[M];
			data = new LDADataset(M);
			data.V = V;
			for (i = 0; i < M; i++) {
				line = reader.readLine();
				String[] tknr = line.split(" ");

				int length = tknr.length;

				z[i] = new Vector<Integer>();
				za[i] = new Vector<Integer>();
				zr[i] = new Vector<Integer>();
				Vector<Integer> words = new Vector<Integer>();

				for (j = 0; j < length; j++) {
					String token = tknr[j];

					words.add(Integer.parseInt(token.split(":")[0]));
					String tmp = token.split(":")[1];
					if (tmp.contains(",")) {
						// System.out.println(tmp);
						String tmp1[] = tmp.split(",");
						za[i].add(Integer.parseInt(tmp1[0]));
						zr[i].add(Integer.parseInt(tmp1[1]));
						z[i].add(Integer.parseInt(tmp1[2]));
					}
				}// end for each topic assignment

				// allocate and add new document to the corpus
				Document doc = new Document(words);
				data.setDoc(doc, i);

				// assign values for z

			}// end for each doc

			reader.close();
		} catch (Exception e) {
			System.out.println("Error while loading model: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * load saved model
	 */

	// ***
	// DON'T USE - HAVE NOT UPDATED THIS METHOD FOR ATM
	// ***
	public boolean loadModel() {
		if (!readOthersFile(dir + File.separator + modelName + othersSuffix))
			return false;

		if (!readTAssignFile(dir + File.separator + modelName + tassignSuffix))
			return false;

		// read dictionary
		Dictionary dict = new Dictionary();
		if (!dict.readWordMap(dir + File.separator + wordMapFile))
			return false;

		data.name2aid = data.readAuthorMap(dir);
		data.localDict = dict;
		A = data.name2aid.size();

		return true;
	}

	/**
	 * Save word-topic assignments for this model
	 */

	public boolean saveModelTAssign(String filename) {
		int i, j;

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			// write docs with topic assignments for words
			for (i = 0; i < data.M; i++) {
				for (j = 0; j < data.docs[i].length; ++j) {
					writer.write(data.docs[i].words[j] + ":" + za[i].get(j)
							+ "," + zr[i].get(j) + "," + z[i].get(j) + ","
							+ " ");
				}
				writer.write("\n");
			}

			writer.close();
		} catch (Exception e) {
			System.out.println("Error while saving model tassign: "
					+ e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Save theta (topic distribution) for this model
	 */
	public boolean saveModelTheta(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			for (int k = 0; k < A; k++) {
				if(askers.contains(k))
				{
					writer.write("Asker "+k+"\n");
					for (int i = 0; i < A; i++) {

						for (int j = 0; j < K; j++) {
							writer.write(theta[k][i][j] + " ");
						}
						writer.write("\n");
					}
				}
				
				writer.newLine();
				writer.newLine();
			}

			writer.close();

		} catch (Exception e) {
			System.out
					.println("Error while saving topic distribution file for this model: "
							+ e.getMessage());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Save word-topic distribution
	 */

	public boolean saveModelPhi(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			for (int i = 0; i < K; i++) {
				for (int j = 0; j < V; j++) {
					writer.write(phi[i][j] + " ");
				}
				writer.write("\n");
			}
			writer.close();
		} catch (Exception e) {
			System.out.println("Error while saving word-topic distribution:"
					+ e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean saveModelPi(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			for (int i = 0; i < A; i++) {
				for (int j = 0; j < A; j++) {
					writer.write(pi[i][j] + " ");
				}
				writer.write("\n");
			}
			
			writer.close();
		} catch (Exception e) {
			System.out.println("Error while saving word-topic distribution:"
					+ e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Save other information of this model
	 */
	public boolean saveModelOthers(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			writer.write("alpha=" + alpha + "\n");
			writer.write("beta=" + beta + "\n");
			writer.write("gamma=" + gamma + "\n");
			writer.write("ntopics=" + K + "\n");
			writer.write("ndocs=" + M + "\n");
			writer.write("nwords=" + V + "\n");
			writer.write("liters=" + liter + "\n");

			writer.close();
		} catch (Exception e) {
			System.out.println("Error while saving model others:"
					+ e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Save model the most likely words for each topic
	 */
	public boolean saveModelTwords(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename), "UTF-8"));

			if (twords > V) {
				twords = V;
			}

			for (int k = 0; k < K; k++) {
				List<Pair> wordsProbsList = new ArrayList<Pair>();
				for (int w = 0; w < V; w++) {
					Pair p = new Pair(w, phi[k][w], false);

					wordsProbsList.add(p);
				}// end foreach word

				// print topic
				writer.write("Topic " + k + "th:\n");
				Collections.sort(wordsProbsList);

				for (int i = 0; i < twords; i++) {
					if (data.localDict
							.contains((Integer) wordsProbsList.get(i).first)) {
						String word = data.localDict
								.getWord((Integer) wordsProbsList.get(i).first);

						writer.write("\t" + word + " "
								+ wordsProbsList.get(i).second + "\n");
					}
				}
			} // end foreach topic

			writer.close();
		} catch (Exception e) {
			System.out.println("Error while saving model twords: "
					+ e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Save model
	 */
	public boolean saveModel(String modelName) {
		if (!saveModelTAssign(dir + File.separator + modelName + tassignSuffix)) {
			return false;
		}

		if (!saveModelOthers(dir + File.separator + modelName + othersSuffix)) {
			return false;
		}

		if (!saveModelTheta(dir + File.separator + modelName + thetaSuffix)) {
			return false;
		}

		if (!saveModelPhi(dir + File.separator + modelName + phiSuffix)) {
			return false;
		}

		if (!saveModelPi(dir + File.separator + modelName + piSuffix)) {
			return false;
		}

		if (twords > 0) {
			if (!saveModelTwords(dir + File.separator + modelName
					+ twordsSuffix))
				return false;
		}
		return true;
	}

	// ---------------------------------------------------------------
	// Init Methods
	// ---------------------------------------------------------------
	/**
	 * initialize the model
	 */
	protected boolean init(LDACmdOption option) {
		if (option == null)
			return false;

		modelName = option.modelName;
		K = option.K;

		alpha = option.alpha;
		if (alpha < 0.0)
			alpha = 50.0 / K;

		if (option.beta >= 0)
			beta = option.beta;

		gamma = option.gamma;

		niters = option.niters;

		dir = option.dir;
		if (dir.endsWith(File.separator))
			dir = dir.substring(0, dir.length() - 1);

		dfile = option.dfile;
		twords = option.twords;
		wordMapFile = option.wordMapFileName;

		return true;
	}

	/**
	 * Init parameters for estimation
	 */
	public boolean initNewModel(LDACmdOption option) {
		if (!init(option))
			return false;

		int m, n, w, k, q;
		
		data = LDADataset.readDataSet(dir + File.separator + dfile, option);
		if (data == null) {
			System.out.println("Fail to read training data!\n");
			return false;
		}

		// + allocate memory and assign values for variables
		M = data.M;
		V = data.V;
		A = data.A;
		dir = option.dir;
		savestep = option.savestep;
		
		nw = new int[V][K];
		for (w = 0; w < V; w++) {
			for (k = 0; k < K; k++) {
				nw[w][k] = 0;
			}
		}

		naa = new int[A][A];
		for (m = 0; m < A; m++) {
			for (k = 0; k < A; k++) {
				naa[m][k] = 0;
			}
		}

		naat = new int[A][A][K];
		for (m = 0; m < A; m++) {
			for (n = 0; n < A; n++) {
				for (k = 0; k < K; k++) {
					naat[m][n][k] = 0;
				}
			}

		}

		nwsum = new int[K];
		for (k = 0; k < K; k++) {
			nwsum[k] = 0;
		}

		naasum = new int[A];
		for (m = 0; m < A; m++) {
			naasum[m] = 0;
		}
		naatsum = new int[A][A];
		for (m = 0; m < A; m++) {
			for (k = 0; k < A; k++) {
				naatsum[m][k] = 0;
			}
		}

		gammaforusers = new double[A][A];
		for (m = 0; m < A; m++) {
			for (n = 0; n < A; n++) {
				gammaforusers[m][n] = gamma;
			}
		}

		ARcount = new int[A][A];
		for (m = 0; m < A; m++) {
			for (n = 0; n < A; n++) {
				ARcount[m][n] = 0;
			}
		}
		// initialize da!
		askers=new Vector<Integer>();
		da = new Vector[M];
		dr = new Vector[M];
		for (m = 0; m < data.M; m++) {
			da[m] = new Vector<Integer>();
			dr[m] = new Vector<Integer>();
			for (int i = 0; i < data.docs[m].authors.length; i++) {
				da[m].add(data.docs[m].authors[i]);
				if (i >= 1) {
					dr[m].add(data.docs[m].authors[i]);
				}
			}
			if(!askers.contains(data.docs[m].authors[0]))
			{
				askers.add(data.docs[m].authors[0]);
			}

		}

		int asker, bestanswerer;
		for (m = 0; m < M; m++) {
			asker = da[m].get(0);
			bestanswerer = da[m].get(1);
			ARcount[asker][bestanswerer] += 10;
			for (n = 2; n < da[m].size(); n++) {
				ARcount[asker][da[m].get(n)] += 1;
			}
		}

		for (m = 0; m < A; m++) {
			for (n = 0; n < A; n++) {
				gammaforusers[m][n] = ARcount[m][n] * gamma;
			}
		}
		// initializing the words with topics
		z = new Vector[M];
		za = new Vector[M];
		zr = new Vector[M];
		double prior[] = new double[A];
		for (m = 0; m < data.M; m++) {
			int N = data.docs[m].length;
			z[m] = new Vector<Integer>();
			za[m] = new Vector<Integer>();
			zr[m] = new Vector<Integer>();
			prior[0] = gammaforusers[da[m].get(0)][0];
			for (q = 1; q < A; q++) {
				prior[q] = prior[q - 1] + gammaforusers[da[m].get(0)][q];
			}
			// initialize for z
			for (n = 0; n < N; n++) {
				w = data.docs[m].words[n];

				int a = da[m].get(0);
				int r = multinomial(prior);
				int topic = (int) Math.floor(Math.random() * K);
				z[m].add(topic);
				za[m].add(a);
				zr[m].add(r);

				nw[data.docs[m].words[n]][topic] += 1;
				naat[a][r][topic] += 1;
				naa[a][r] += 1;
				nwsum[topic] += 1;
				naasum[a] += 1;
				naatsum[a][r] += 1;
			}
			//                     
		}

		theta = new double[A][A][K];
		phi = new double[K][V];
		pi = new double[A][A];
		return true;
	}

	/**
	 * Init parameters for inference
	 * 
	 * @param newData
	 *            DataSet for which we do inference
	 */
	public boolean initNewModel(LDACmdOption option, LDADataset newData,
			Model trnModel) {
		if (!init(option))
			return false;

		int m, n, w, k;

		K = trnModel.K;
		alpha = trnModel.alpha;
		beta = trnModel.beta;
		gamma = trnModel.gamma;
		
		System.out.println("K:" + K);

		data = newData;
		A = data.A;
		// + allocate memory and assign values for variables
		M = data.M;
		V = data.V;
		dir = option.dir;
		savestep = option.savestep;
		System.out.println("M:" + M);
		System.out.println("V:" + V);
		System.out.println("A:" + A);
		// K: from command line or default value
		// alpha, beta: from command line or default values
		// niters, savestep: from command line or default values

		nw = new int[V][K];
		for (w = 0; w < V; w++) {
			for (k = 0; k < K; k++) {
				nw[w][k] = 0;
			}
		}

		naa = new int[A][A];
		for (m = 0; m < A; m++) {
			for (k = 0; k < A; k++) {
				naa[m][k] = 0;
			}
		}

		naat = new int[A][A][K];
		for (m = 0; m < A; m++) {
			for (n = 0; n < A; n++) {
				for (k = 0; k < K; k++) {
					naat[m][n][k] = 0;
				}
			}
		}
		gammaforusers = new double[A][A];
		for (m = 0; m < A; m++) {
			for (n = 0; n < A; n++) {
				gammaforusers[m][n] = gamma;
			}
		}
		nwsum = new int[K];
		for (k = 0; k < K; k++) {
			nwsum[k] = 0;
		}

		naasum = new int[A];
		for (m = 0; m < A; m++) {
			naasum[m] = 0;
		}
		naatsum = new int[A][A];
		for (m = 0; m < A; m++) {
			for (k = 0; k < A; k++) {
				naatsum[m][k] = 0;
			}
		}
		askers=new Vector<Integer>();
		dr = new Vector[M];
		for (m = 0; m < data.M; m++) {
			dr[m] = new Vector<Integer>();
			for (int i = 1; i < data.docs[m].authors.length; i++) {
				dr[m].add(data.docs[m].authors[i]);

			}
			if(!askers.contains(data.docs[m].authors[0]))
			{
				askers.add(data.docs[m].authors[0]);
			}
		}
		z = new Vector[M];
		za = new Vector[M];
		zr = new Vector[M];
		for (m = 0; m < data.M; m++) {
			int N = data.docs[m].length;
			z[m] = new Vector<Integer>();
			za[m] = new Vector<Integer>();
			zr[m] = new Vector<Integer>();
			// initialize for z
			int a = data.docs[m].authors[0];
			for (n = 0; n < N; n++) {
				int topic = (int) Math.floor(Math.random() * K);
				int r = dr[m].get((int) Math
						.floor(Math.random() * dr[m].size()));

				z[m].add(topic);
				za[m].add(a);
				zr[m].add(r);
				if (data.la2ga.get(a) != null && data.la2ga.get(r) != null) {
					gammaforusers[a][r] = trnModel.ARcount[data.la2ga.get(a)][data.la2ga
							.get(r)]
							* gammaforusers[a][r];
				}
				nw[data.docs[m].words[n]][topic] += 1;
				naat[a][r][topic] += 1;
				naa[a][r] += 1;
				nwsum[topic] += 1;
				naasum[a] += 1;
				naatsum[a][r] += 1;
			}

		}

		theta = new double[A][A][K];
		phi = new double[K][V];
		pi = new double[A][A];
		return true;
	}

	/**
	 * Init parameters for inference reading new dataset from file
	 */

	// ***
	// DON'T USE - HAVE NOT UPDATED THIS METHOD FOR ATM
	// ***
	public boolean initNewModel(LDACmdOption option, Model trnModel) {
		if (!init(option))
			return false;

		LDADataset dataset = LDADataset.readDataSet(dir + File.separator
				+ dfile, trnModel.data.localDict,
				(HashMap<String, Integer>) trnModel.data.name2aid, option.dir);
		if (dataset == null) {
			System.out.println("Fail to read dataset!\n");
			return false;
		}

		return initNewModel(option, dataset, trnModel);
	}

	/**
	 * init parameter for continue estimating or for later inference
	 */

	// ***
	// DON'T USE - HAVE NOT UPDATED THIS METHOD FOR ATM
	// ***
	public boolean initEstimatedModel(LDACmdOption option) {
		if (!init(option))
			return false;

		int m, n, w, k;

		// load model, i.e., read z and trndata
		if (!loadModel()) {
			System.out
					.println("Fail to load word-topic assignment file of the model!\n");
			return false;
		}

		System.out.println("Model loaded:");
		System.out.println("\talpha:" + alpha);
		System.out.println("\tbeta:" + beta);
		System.out.println("\tM:" + M);
		System.out.println("\tV:" + V);
		System.out.println("\tA:" + A);
		nw = new int[V][K];
		for (w = 0; w < V; w++) {
			for (k = 0; k < K; k++) {
				nw[w][k] = 0;
			}
		}

		naa = new int[A][A];
		for (m = 0; m < A; m++) {
			for (k = 0; k < A; k++) {
				naa[m][k] = 0;
			}
		}

		naat = new int[A][A][K];
		for (m = 0; m < A; m++) {
			for (n = 0; n < A; n++) {
				for (k = 0; k < K; k++) {
					naat[m][n][k] = 0;
				}
			}
		}

		ARcount = new int[A][A];
		for (m = 0; m < A; m++) {
			for (n = 0; n < A; n++) {
				ARcount[m][n] = 0;
			}
		}

		nwsum = new int[K];
		for (k = 0; k < K; k++) {
			nwsum[k] = 0;
		}

		naasum = new int[A];
		for (m = 0; m < A; m++) {
			naasum[m] = 0;
		}
		naatsum = new int[A][A];
		for (m = 0; m < A; m++) {
			for (k = 0; k < A; k++) {
				naatsum[m][k] = 0;
			}
		}
		for (m = 0; m < data.M; m++) {
			int N = data.docs[m].length;

			// assign values for nw, nd, nwsum, and ndsum
			for (n = 0; n < N; n++) {
				w = data.docs[m].words[n];
				int topic = (Integer) z[m].get(n);
				int a = za[m].get(n);
				int r = zr[m].get(n);
				ARcount[a][r] += 1;
				nw[w][topic] += 1;
				naat[a][r][topic] += 1;
				naa[a][r] += 1;
				nwsum[topic] += 1;
				naasum[a] += 1;
				naatsum[a][r] += 1;
			}
			// total number of words in document i

		}
		theta = new double[A][A][K];
		phi = new double[K][V];
		pi = new double[A][A];
		dir = option.dir;
		savestep = option.savestep;

		return true;
	}

	public int multinomial(double prior[]) {
		double t = Math.random() * prior[prior.length - 1];

		return binarysearch(prior, 0, prior.length - 1, t);
	}

	public int binarysearch(double[] v, int s, int f, double u) {
		int k = (s + f) / 2;
		if (v[s] >= u) {
			return s;
		}

		else if (v[k] < u && v[k + 1] >= u) {
			return k + 1;
		} else if (v[k] >= u && k != 0) {
			return binarysearch(v, s, k, u);
		} else {
			return binarysearch(v, k + 1, f, u);
		}

	}

}