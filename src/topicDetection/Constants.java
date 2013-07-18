package topicDetection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.StringTokenizer;

public class Constants {

	public Constants()throws Exception{
		load(new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("TDTConstants.txt"))));
	}

	public Constants(String constantsFile) throws Exception {
		load(new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(constantsFile))));
	}

	public Constants(BufferedReader constantsFile) throws Exception {
		load(constantsFile);
	}

	// -- default values ---
	public boolean KEYWORDS_1_ENABLE = true;
	public boolean KEYWORDS_2_ENABLE = true;
	public boolean TEXT_ENABLE = true;
	
	public boolean REMOVE_DUPLICATES = false;

	public double KEYWORDS_1_WEIGHT = 1;
	public double KEYWORDS_2_WEIGHT = 1;
	public double TEXT_WEIGHT = 1;

	public boolean HARD_CLUSTERING = false;

	public int CENTROID_KEYWORD_DF_MIN = 3;
	public int SIMILARITY_KEYWORD_DF_MIN = 5;

	public int NODE_DF_MIN = 4;
	public double NODE_DF_MAX = 150.0 / 48 / 21;

	public double EDGE_CORRELATION_MIN = .15;
	public int EDGE_DF_MIN = 3;
	public double EDGE_CP_MIN_TO_DUPLICATE = 1.7;

	public int DOC_KEYWORDS_SIZE_MIN = 5;
	public int DOC_CHAR_SIZE_MAX = 2000;
	public double DOC_SIM2CENTROID_MIN = 0;// 0.001;
	public double DOC_SIM2KEYGRAPH_MIN = .2523;

	public int CLUSTER_NODE_SIZE_MAX = 900;
	public int CLUSTER_NODE_SIZE_MIN = 3;
	public double CLUSTER_VAR_MAX = 1.99;
	public double CLUSTER_INTERSECT_MIN = .1;

	public int TOPIC_MAX = 1000;
	public int TOPIC_MIN_SIZE = 5;

	public String CLUSTERING_ALG = "Betweenness";

	public String DATA_TOPIC_PATH = "./data/topic/";
	public String DATA_KEYWORDS_2_PATH = "./data/keywords2/";
	public String DATA_KEYWORDS_1_PATH = "./data/keywords1/";
	public String DATA_TEXT_PATH = "./data/text/";
	public String DATA_DATE_PATH = "/fs/clip-clip-proj/GeoNets/hassan/workspace/dataProcess/data/largeData_date";

	public void load(BufferedReader in) throws Exception {
		// System.out.println(new File(constantsFile).getAbsolutePath());
		// DataInputStream in=new DataInputStream(new
		// FileInputStream(constantsFile));
		HashMap<String, String> conf = new HashMap<String, String>();
		String line = null;
		while ((line = in.readLine()) != null) {
			line = line.trim();
			if (line.startsWith("//") || line.length() == 0)
				continue;

			StringTokenizer st = new StringTokenizer(line, "= ;");
			// System.out.println(line);
			conf.put(st.nextToken(), st.nextToken());
		}

		KEYWORDS_1_ENABLE = Boolean.parseBoolean(conf.get("KEYWORDS_1_ENABLE"));
		KEYWORDS_2_ENABLE = Boolean.parseBoolean(conf.get("KEYWORDS_2_ENABLE"));
		TEXT_ENABLE = Boolean.parseBoolean(conf.get("TEXT_ENABLE"));
		
		

		KEYWORDS_1_WEIGHT = Double.parseDouble(conf.get("KEYWORDS_WEIGHT"));
		KEYWORDS_2_WEIGHT = Double.parseDouble(conf.get("KEYWORDS_2_WEIGHT"));
		TEXT_WEIGHT = Double.parseDouble(conf.get("TEXT_WEIGHT"));

		HARD_CLUSTERING = Boolean.parseBoolean(conf.get("HARD_CLUSTERING"));
		REMOVE_DUPLICATES = Boolean.parseBoolean(conf.get("REMOVE_DUPLICATES"));

		CENTROID_KEYWORD_DF_MIN = Integer.parseInt(conf.get("CENTROID_KEYWORD_DF_MIN"));
		SIMILARITY_KEYWORD_DF_MIN = Integer.parseInt(conf.get("SIMILARITY_KEYWORD_DF_MIN"));

		NODE_DF_MIN = Integer.parseInt(conf.get("NODE_DF_MIN"));
		NODE_DF_MAX = Double.parseDouble(conf.get("NODE_DF_MAX"));

		EDGE_CORRELATION_MIN = Double.parseDouble(conf.get("EDGE_CORRELATION_MIN"));
		EDGE_DF_MIN = Integer.parseInt(conf.get("EDGE_DF_MIN"));
		EDGE_CP_MIN_TO_DUPLICATE = Double.parseDouble(conf.get("EDGE_CP_MIN_TO_DUPLICATE"));

		DOC_KEYWORDS_SIZE_MIN = Integer.parseInt(conf.get("DOC_KEYWORDS_SIZE_MIN"));
		DOC_CHAR_SIZE_MAX = Integer.parseInt(conf.get("DOC_CHAR_SIZE_MAX"));
		DOC_SIM2CENTROID_MIN = Double.parseDouble(conf.get("DOC_SIM2CENTROID_MIN"));
		DOC_SIM2KEYGRAPH_MIN = Double.parseDouble(conf.get("DOC_SIM2KEYGRAPH_MIN"));

		CLUSTER_NODE_SIZE_MAX = Integer.parseInt(conf.get("CLUSTER_NODE_SIZE_MAX"));
		CLUSTER_NODE_SIZE_MIN = Integer.parseInt(conf.get("CLUSTER_NODE_SIZE_MIN"));
		CLUSTER_VAR_MAX = Double.parseDouble(conf.get("CLUSTER_VAR_MAX"));
		CLUSTER_INTERSECT_MIN = Double.parseDouble(conf.get("CLUSTER_INTERSECT_MIN"));

		TOPIC_MAX = Integer.parseInt(conf.get("TOPIC_MAX"));
		TOPIC_MIN_SIZE = Integer.parseInt(conf.get("TOPIC_MIN_SIZE"));

		CLUSTERING_ALG = conf.get("CLUSTERING_ALG");

		DATA_TOPIC_PATH = conf.get("DATA_TOPIC_PATH");
		DATA_KEYWORDS_2_PATH = conf.get("DATA_KEYWORDS_2_PATH");
		DATA_KEYWORDS_1_PATH = conf.get("DATA_KEYWORDS_1_PATH");
		DATA_TEXT_PATH = conf.get("DATA_TEXT_PATH");
		DATA_DATE_PATH = conf.get("DATA_DATE_PATH");
	}
}
