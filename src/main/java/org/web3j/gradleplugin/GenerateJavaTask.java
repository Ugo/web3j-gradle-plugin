package org.web3j.gradleplugin;

import org.ethereum.solidity.compiler.SolidityCompiler;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.codegen.SolidityFunctionWrapper;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;


public class GenerateJavaTask extends DefaultTask {

    // TODO: this should be changed to a folder
    private String contractName;

    // TODO: blank for now, this doesn't work when we put a value here
    private static final String DEFAULT_PACKAGE = "";
    private static final String DEFAULT_SOURCE_DESTINATION = "src/main/java";
    private static final String DEFAULT_SOLIDITY_SOURCES = "src/main/resources";

    private final boolean nativeJavaType = true;

    // this should be set
    private String javaPackageName = DEFAULT_PACKAGE;
    private String javaDestinationFolder = DEFAULT_SOURCE_DESTINATION;

    private static final Logger log = LoggerFactory.getLogger(GenerateJavaTask.class);

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    @TaskAction
    void action() throws Exception {

        // solidity file
        String contractPath = DEFAULT_SOLIDITY_SOURCES + "/" + getContractName();

        Map<String, Map<String, String>> contracts = getCompiledContract(contractPath);
        if (contracts == null) {
            log.warn("\tNo Contract found for file '" + contractPath + "'");
            return;
        }
        for (String contractName : contracts.keySet()) {
            try {
                log.info("\tTry to build java class for contract '" + contractName + "'");
                generateJavaClass(contracts, contractName);
                log.info("\tBuilt Class for contract '" + contractName + "'");
            } catch (Exception e) {
                log.error("Could not build java class for contract '" + contractName + "'", e);
            }
        }
    }

    private Map<String, Map<String, String>> getCompiledContract(String contractPath) throws Exception {

        File f = new File(contractPath);
        if(!f.exists() || f.isDirectory()) {
            return null;
        }

        String result = compileSolidityContract(contractPath);
        // TODO: for some reason a stdin is added to the contract name, removing it the ugly way for now
        result = result.replaceAll("<stdin>:", "");

        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
            String script = "JSON.parse(JSON.stringify(" + result + "))";
            Map<String, Object> json = (Map<String, Object>) engine.eval(script);
            return (Map<String, Map<String, String>>) json.get("contracts");
        } catch (ScriptException e) {
            throw new Exception("Could not parse SolC result", e);
        }
    }

    private String compileSolidityContract(String contractPath) throws Exception {
        try {
            byte[] contract = Files.readAllBytes(Paths.get(contractPath));
            SolidityCompiler.Result result = SolidityCompiler.getInstance().compileSrc(
                    contract,
                    true,
                    true,
                    SolidityCompiler.Options.ABI,
                    SolidityCompiler.Options.BIN,
                    SolidityCompiler.Options.INTERFACE,
                    SolidityCompiler.Options.METADATA
            );
            if (result.isFailed()) {
                throw new Exception("Could not compile solidity files\n" + result.errors);
            }

            return result.output;
        } catch (IOException ioException) {
            throw new Exception("Could not compile files", ioException);
        }
    }

    private void generateJavaClass(Map<String, Map<String, String>> result, String contractName) throws IOException, ClassNotFoundException {

        new SolidityFunctionWrapper(nativeJavaType).generateJavaFiles(
                contractName,
                result.get(contractName).get("bin"),
                result.get(contractName).get("abi"),
                javaDestinationFolder,
                javaPackageName);
    }
}
