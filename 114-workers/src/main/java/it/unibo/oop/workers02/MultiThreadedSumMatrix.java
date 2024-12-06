package it.unibo.oop.workers02;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an implementation of the calculation.
 * 
 */
public final class MultiThreadedSumMatrix implements SumMatrix {

    private final int nthread;

    /**
     * 
     * @param nthread
     *            no. of thread performing the sum.
     */
    public MultiThreadedSumMatrix(final int nthread) {
        this.nthread = nthread;
    }

    @Override
    public double sum(final double[][] matrixInput) {
        final int rows = matrixInput.length;
        final int cols = matrixInput[0].length;
        final int size = rows * cols % nthread + rows * cols / nthread;
        final List<List<Double>> matrix;
        matrix = new ArrayList<>(rows);
        for (int i = 0; i < rows; i++) {
            matrix.add(new ArrayList<>(cols));
            for (int j = 0; j < cols; j++) {
                matrix.get(i).add(matrixInput[i][j]);
            }
        }
        /*
         * Build a list of workers
         */
        final List<Worker> workers = new ArrayList<>(nthread);
        for (int start = 0; start < rows * cols; start += size) {
            workers.add(new Worker(matrix, start, size));
        }
        /*
         * Start them
         */
        for (final Worker w: workers) {
            w.start();
        }
        /*
         * Wait for every one of them to finish. This operation is _way_ better done by
         * using barriers and latches, and the whole operation would be better done with
         * futures.
         */
        long sum = 0;
        for (final Worker w: workers) {
            try {
                w.join();
                sum += w.getResult();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
        /*
         * Return the sum
         */
        return sum;
    }

    private static class Worker extends Thread {
        private final List<List<Double>> matrix;
        private final int startpos;
        private final int nelem;
        private double res;

        /**
         * Build a new worker.
         * 
         * @param matrix
         *            the matrix to sum
         * @param startpos
         *            the initial position for this worker
         * @param nelem
         *            the no. of elems to sum up for this worker
         */
        Worker(final List<List<Double>> matrix, final int startpos, final int nelem) {
            super();
            this.matrix = matrix;
            this.startpos = startpos;
            this.nelem = nelem;
        }

        @Override
        @SuppressWarnings("PMD.SystemPrintln")
        public void run() {
            final int rows = matrix.size();
            final int cols = matrix.get(0).size();
            System.out.println(
                "Working from position ["
                + computeRow(startpos, cols) + "][" + computeColumn(startpos, cols) + "] " 
                + "to position ["
                + computeRow(startpos + nelem - 1, cols) + "][" + computeColumn(startpos + nelem - 1, cols) + "]" 
            );
            for (int i = startpos; i < rows * cols && i < startpos + nelem; i++) {
                this.res += this.matrix.get(computeRow(i, cols)).get(computeColumn(i, cols));
            }
        }

        /**
         * Returns the result of summing up the doubles within the matrix.
         * 
         * @return the sum of every element in the matrix
         */
        public double getResult() {
            return this.res;
        }

        private static int computeRow(final int pos, final int columns) {
            return pos / columns;
        }

        private static int computeColumn(final int pos, final int columns) {
            return pos % columns;
        }
    }
}
