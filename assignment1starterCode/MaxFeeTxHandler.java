import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaxFeeTxHandler extends TxHandler {
    private UTXOPool utxoPool;

    public MaxFeeTxHandler(UTXOPool utxoPool) {
        super(utxoPool);
        this.utxoPool = new UTXOPool(utxoPool);
    }

    @Override
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        if (possibleTxs == null || possibleTxs.length == 0) {
            return new Transaction[]{};
        }

        // 存儲每個交易的手續費
        Map<Transaction, Double> txFees = new HashMap<>();
        // 計算每個交易的手續費
        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                double fee = calculateFee(tx);
                txFees.put(tx, fee);
            }
        }

        // 使用動態規劃求解最大手續費組合
        List<Transaction> maxFeeSet = findMaxFeeSet(possibleTxs, txFees);

        // 更新UTXO池
        for (Transaction tx : maxFeeSet) {
            // 移除已使用的UTXO
            for (Transaction.Input input : tx.getInputs()) {
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                utxoPool.removeUTXO(utxo);
            }

            // 添加新的UTXO
            byte[] txHash = tx.getHash();
            for (int i = 0; i < tx.numOutputs(); i++) {
                Transaction.Output output = tx.getOutput(i);
                UTXO utxo = new UTXO(txHash, i);
                utxoPool.addUTXO(utxo, output);
            }
        }

        return maxFeeSet.toArray(new Transaction[0]);
    }

    private double calculateFee(Transaction tx) {
        double totalInput = 0;
        double totalOutput = 0;

        // 計算輸入總額
        for (Transaction.Input input : tx.getInputs()) {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (utxoPool.contains(utxo)) {
                totalInput += utxoPool.getTxOutput(utxo).value;
            }
        }

        // 計算輸出總額
        for (Transaction.Output output : tx.getOutputs()) {
            totalOutput += output.value;
        }

        return totalInput - totalOutput;
    }

    private List<Transaction> findMaxFeeSet(Transaction[] possibleTxs, Map<Transaction, Double> txFees) {
        int n = possibleTxs.length;
        // dp[i]存儲到第i個交易為止的最大手續費組合
        double[] dp = new double[1 << n];  // 2^n種可能的組合
        int[] prev = new int[1 << n];      // 追踪最優解的路徑

        // 對每種可能的組合進行計算
        for (int mask = 1; mask < (1 << n); mask++) {
            double currentFee = 0;
            boolean isValid = true;
            UTXOPool tempPool = new UTXOPool(utxoPool);

            // 檢查當前組合中的每個交易
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {  // 如果第i個交易在當前組合中
                    Transaction tx = possibleTxs[i];

                    // 檢查交易在當前UTXO池的狀態下是否有效
                    if (!isValidTx(tx)) {
                        isValid = false;
                        break;
                    }

                    // 更新臨時UTXO池
                    for (Transaction.Input input : tx.getInputs()) {
                        UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                        tempPool.removeUTXO(utxo);
                    }

                    currentFee += txFees.getOrDefault(tx, 0.0);
                }
            }

            if (isValid && currentFee > dp[mask]) {
                dp[mask] = currentFee;
                prev[mask] = mask;
            }
        }

        // 找出最大手續費的組合
        int maxMask = 0;
        for (int mask = 0; mask < (1 << n); mask++) {
            if (dp[mask] > dp[maxMask]) {
                maxMask = mask;
            }
        }

        // 構建結果列表
        List<Transaction> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if ((maxMask & (1 << i)) != 0) {
                result.add(possibleTxs[i]);
            }
        }

        return result;
    }
}