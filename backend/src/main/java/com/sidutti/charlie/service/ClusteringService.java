package com.sidutti.charlie.service;

import com.sidutti.charlie.repository.elastic.ElasticDocumentRepository;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ClusteringService {
    private final ElasticDocumentRepository repository;

    public ClusteringService(ElasticDocumentRepository repository) {
        this.repository = repository;
    }

    public void clusterDocuments() {
        KMeansPlusPlusClusterer<DoublePoint> clusterer = new KMeansPlusPlusClusterer<>(500, 10, new EuclideanDistance());
        List<DoublePoint> data = new ArrayList<>();
        repository.findAll();

        List<CentroidCluster<DoublePoint>> clusters = clusterer.cluster(data);

// Print the cluster assignments
        for (CentroidCluster<DoublePoint> cluster : clusters) {
            System.out.println("Cluster: " + cluster.getCenter());
            for (DoublePoint point : cluster.getPoints()) {
                System.out.println("  Point: " + point);
            }
        }
    }
}
