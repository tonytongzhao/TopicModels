package jGibbATM;

import org.kohsuke.args4j.*;

public class ATM {
        
        public static void main(String args[]){
                LDACmdOption option = new LDACmdOption();
                CmdLineParser parser = new CmdLineParser(option);
                
                try {
                        if (args.length == 0){
                                showHelp(parser);
                                return;
                        }
                        
                        parser.parseArgument(args);
                        
                        if (option.est || option.estc){
                                Estimator estimator = new Estimator();
                                estimator.init(option);
                                estimator.estimate();
                        }
                        else if (option.inf){
                                Inferencer inferencer = new Inferencer();
                                inferencer.init(option);
                                
                                Model newModel = inferencer.inference();
                        
                               
                        }
                }
                catch (CmdLineException cle){
                        System.out.println("Command line error: " + cle.getMessage());
                        showHelp(parser);
                        return;
                }
                catch (Exception e){
                        System.out.println("Error in main: " + e.getMessage());
                        e.printStackTrace();
                        return;
                }
        }
        
        public static void showHelp(CmdLineParser parser){
                System.out.println("LDA [options ...] [arguments...]");
                parser.printUsage(System.out);
        }
        
}
