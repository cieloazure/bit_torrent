package misc;

public class CommonConfig {
    private Integer numOfPreferredNeighbours;
    private Integer unchokingInterval;
    private Integer optimisticUnchokingInterval;
    private String fileName;
    private long fileSize;
    private long pieceSize;

    private CommonConfig(CommonConfig.Builder builder) {
        this.numOfPreferredNeighbours = builder.numOfPreferredNeighbours;
        this.unchokingInterval = builder.unchokingInterval;
        this.optimisticUnchokingInterval = builder.optimisticUnchokingInterval;
        this.fileName = builder.fileName;
        this.fileSize = builder.fileSize;
        this.pieceSize = builder.pieceSize;
    }

    public Integer getNumOfPreferredNeighbours() {
        return numOfPreferredNeighbours;
    }

    public Integer getUnchokingInterval() {
        return unchokingInterval;
    }

    public Integer getOptimisticUnchokingInterval() {
        return optimisticUnchokingInterval;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getPieceSize() {
        return pieceSize;
    }

    public static class Builder {
        private Integer numOfPreferredNeighbours;
        private Integer unchokingInterval;
        private Integer optimisticUnchokingInterval;
        private String fileName;
        private long fileSize;
        private long pieceSize;

        public Builder() {
        }

        public Builder withNumOfPreferredNeighboursAs(Integer numOfPreferredNeighbours) {
            this.numOfPreferredNeighbours = numOfPreferredNeighbours;
            return this;
        }

        public Builder withUnchokingIntervalAs(Integer unchokingInterval) {
            this.unchokingInterval = unchokingInterval;
            return this;
        }

        public Builder withOptimisticUnchokingIntervalAs(Integer optimisticUnchokingInterval) {
            this.optimisticUnchokingInterval = optimisticUnchokingInterval;
            return this;
        }

        public Builder withFileParametersAs(String fileName, long fileSize, long pieceSize) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.pieceSize = pieceSize;
            return this;
        }

        public CommonConfig build() {
            return new CommonConfig(this);
        }
    }
}
