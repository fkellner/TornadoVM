/*
 * Copyright (c) 2022, APT Group, Department of Computer Science,
 * The University of Manchester.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.manchester.tornado.examples.compute;

import java.util.ArrayList;
import java.util.LongSummaryStatistics;
import java.util.Random;
import java.util.stream.IntStream;

import uk.ac.manchester.tornado.api.TaskSchedule;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.collections.types.Matrix2DFloat;
import uk.ac.manchester.tornado.api.collections.types.VectorFloat;

/**
 * Linear-Algebra example: Matrix-Vector.
 *
 * How to run?
 *
 * <code>
 *     $ # To run with level-zero and SPIR-V
 *     $ tornado --jvm="-Dla.mv.device=0:0 -Dtornado.device.memory=24GB" -m tornado.examples/uk.ac.manchester.tornado.examples.compute.MatrixVector
 * </code>
 *
 * If this example is executed on a discrete GPU, then we need to decrease the
 * data size (maximum allocation size is usually 1/4 of total GPU memory's
 * capacity. How to run with the TornadoVM profiler?
 *
 * Run with the profiler:
 *
 * <code>
 *     $ tornado --enableProfiler console -m tornado.examples/uk.ac.manchester.tornado.examples.compute.MatrixVector
 * </code>
 *
 */
public class MatrixVector {

    public static final int WARM_UP_ITERATIONS = 100;
    public static final int MAX_ITERATIONS = 100;

    private static void computeMatrixVector(Matrix2DFloat matrix, VectorFloat vector, VectorFloat output) {
        for (@Parallel int i = 0; i < vector.size(); i++) {
            float sum = 0.0f;
            for (int j = 0; j < matrix.N(); j++) {
                sum += vector.get(i) * matrix.get(i, i);
            }
            output.set(i, sum);
        }
    }

    public static void main(String[] args) {

        int size = 8192;
        if (args.length >= 1) {
            try {
                size = Integer.parseInt(args[0]);
            } catch (NumberFormatException numberFormatException) {
                size = 8192;
            }
        }

        // Create a matrix of M rows and N columns (MxN)
        Matrix2DFloat matrix2DFloat = new Matrix2DFloat(size, size);

        // Vector must be of size N
        VectorFloat vectorFloat = new VectorFloat(size);

        // Output
        VectorFloat result = new VectorFloat(size);

        VectorFloat resultSeq = new VectorFloat(size);

        Random r = new Random();

        ArrayList<Long> seqTimers = new ArrayList<>();
        ArrayList<Long> tornadoTimers = new ArrayList<>();

        final int s = size;

        // Init Data
        IntStream.range(0, size).forEach(idx -> vectorFloat.set(idx, r.nextFloat()));
        IntStream.range(0, size).forEach(idx -> IntStream.range(0, s).forEach(jdx -> {
            matrix2DFloat.set(idx, jdx, r.nextFloat());
        }));

        TaskSchedule ts = new TaskSchedule("la") //
                // .streamIn(matrix2DFloat, vectorFloat) //
                .lockObjectsInMemory(matrix2DFloat, vectorFloat, result) //
                .task("mv", MatrixVector::computeMatrixVector, matrix2DFloat, vectorFloat, result) //
                .streamOut(result);

        ts.warmup();

        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            computeMatrixVector(matrix2DFloat, vectorFloat, resultSeq);
        }

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            long start = System.nanoTime();
            computeMatrixVector(matrix2DFloat, vectorFloat, resultSeq);
            long end = System.nanoTime();
            seqTimers.add((end - start));
            System.out.println("SEQ-TIME: " + (end - start));
        }

        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            ts.execute();
        }

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            long start = System.nanoTime();
            ts.execute();
            long end = System.nanoTime();
            tornadoTimers.add((end - start));
            System.out.println("PARALLEL-TIME: " + (end - start));
        }

        // Compute Medians
        LongSummaryStatistics statsSeq = seqTimers.stream().mapToLong(Long::longValue).summaryStatistics();
        LongSummaryStatistics statsTornado = tornadoTimers.stream().mapToLong(Long::longValue).summaryStatistics();

        System.out.println("SEQ    : " + statsSeq.getAverage());
        System.out.println("Tornado: " + statsTornado.getAverage());
        System.out.println("SPEEDUP: " + (statsSeq.getAverage() / statsTornado.getAverage()));
    }
}
