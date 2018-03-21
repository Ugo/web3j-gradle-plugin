package org.web3j.gradleplugin;

import org.ethereum.solidity.compiler.SolidityCompiler;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.web3j.codegen.SolidityFunctionWrapper;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;


public class SolcCompilerTask extends DefaultTask {

    private static final String DEFAULT_SOURCE_DESTINATION = "src/main/java";
    private static final String DEFAULT_SOLIDITY_SOURCES = "src/main/resources";

    private final boolean nativeJavaType = true;

    protected Contract contract;

    // this is where the action of the plugin is performed
    @TaskAction
    void action() throws Exception {

        // solidity file:
        String file = "path/to.file";
        String result = parseSoliditySource(file);
        Map<String, Map<String, String>> contracts = extractContracts(result);
        if (contracts == null) {
            // getLog().warn("\tNo Contract found for file '" + includedFile + "'");
            return;
        }
        for (String contractName : contracts.keySet()) {
            if (isFiltered(contractName)) {
                // getLog().debug("\tContract '" + contractName + "' is filtered");
                continue;
            }
            try {
                generatedJavaClass(contracts, contractName);
                // getLog().info("\tBuilt Class for contract '" + contractName + "'");
            } catch (ClassNotFoundException | IOException ioException) {
                // getLog().error("Could not build java class for contract '" + contractName + "'", ioException);
            }
        }
    }

    private String parseSoliditySource(String includedFile) throws Exception {
        try {
            // byte[] contract = Files.readAllBytes(Paths.get(soliditySourceFiles.getDirectory(), includedFile));
            byte[] contract = Files.readAllBytes(Paths.get("ertre"));
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

            // getLog().debug("\t\tResult:\t" + result.output);
            // getLog().debug("\t\tError: \t" + result.errors);
            return result.output;
        } catch (IOException ioException) {
            throw new Exception("Could not compile files", ioException);
        }
    }

    private Map<String, Map<String, String>> extractContracts(String result) throws Exception{
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
            String script = "JSON.parse(JSON.stringify(" + result + "))";
            Map<String, Object> json = (Map<String, Object>) engine.eval(script);
            return (Map<String, Map<String, String>>) json.get("contracts");
        } catch (ScriptException e) {
            throw new Exception("Could not parse SolC result", e);
        }
    }

    private void generatedJavaClass(Map<String, Map<String, String>> result, String contractName) throws IOException, ClassNotFoundException {
        /**new SolidityFunctionWrapper(nativeJavaType).generateJavaFiles(
                contractName,
                result.get(contractName).get(SolidityCompiler.Options.BIN.getName()),
                result.get(contractName).get(SolidityCompiler.Options.ABI.getName()),
                sourceDestination,
                packageName);*/
    }

    private boolean isFiltered(String contractName) {
        if (contract == null) {
            return false;
        }

        if (contract.getExcludes() != null && !contract.getExcludes().isEmpty()) {
            return contract.getExcludes().contains(contractName);
        }

        if (contract.getIncludes() == null || contract.getIncludes().isEmpty()) {
            return false;
        } else {
            return !contract.getIncludes().contains(contractName);
        }
    }
}
