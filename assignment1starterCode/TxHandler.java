import java.security.PublicKey;
import java.util.ArrayList;

public class TxHandler {
        private UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        ArrayList<UTXO> usedUTXOs = new ArrayList<>();
        double totalInput = 0;

        for (int i = 0; i < tx.numInputs(); i++){
            Transaction.Input in = tx.getInput(i);

            //創建UXTO對象以檢查是否在池中
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            //條件1 檢查UTXO是否在當前池中
            if (!utxoPool.contains(utxo)){
                return false;
            }

            //條件2 驗證簽名
            Transaction.Output out = utxoPool.getTxOutput(utxo);
            PublicKey publicKey = out.address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] sign = in.signature;
            if (!Crypto.verifySignature(publicKey, message, sign)){
                return false;
            }

            //條件3 檢查UTXO是否已被使用
            if (usedUTXOs.contains(utxo)){
                return false;
            }
            usedUTXOs.add(utxo);

            //累加金額
            totalInput += out.value;
        }

        //計算輸出總額並檢查條件4和5
        double totalOutput = 0;
        for (int i = 0; i < tx.numOutputs(); i++){
            Transaction.Output out = tx.getOutput(i);

            //條件4 檢查輸出是否為負數
            if (out.value < 0){
                return false;
            }
            totalOutput += out.value;
        }
        //條件5 檢查輸入是否大於等於輸出
        if(totalInput < totalOutput){
            return false;
        }
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTxs  = new ArrayList<>();

        //追蹤這一輪已經處理過的交易
        boolean[] processed = new boolean[possibleTxs.length];
        boolean changed;

        do {
            changed = false;
            //檢查每個未處理的交易
            for (int i = 0; i < possibleTxs.length; i++) {
                if (!processed[i]) {
                    Transaction tx = possibleTxs[i];

                    //驗證交易
                    if (isValidTx(tx)){
                        validTxs.add(tx);
                        processed[i] = true;
                        changed = true;

                        //更新UTXO池
                        // 1. 移除已使用的UTXO
                        for (Transaction.Input in : tx.getInputs()) {
                            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                            utxoPool.removeUTXO(utxo);
                        }

                        //2. 添加新的UTXO
                        byte[] txHash = tx.getHash();
                        for (int j = 0; j < tx.numInputs(); j++) {
                            Transaction.Output out = tx.getOutput(j);
                            UTXO utxo = new UTXO(txHash, j);
                            utxoPool.addUTXO(utxo, out);
                        }
                    }
                }
            }
        } while (changed); //繼續找到沒有新的有效交易被找到為止

        //將ArrayList轉換為數組返回
        return validTxs.toArray(new Transaction[validTxs.size()]);
    }

}
