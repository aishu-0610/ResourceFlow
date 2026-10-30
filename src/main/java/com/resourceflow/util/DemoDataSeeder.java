package com.resourceflow.util;

import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.RequestStatus;
import com.resourceflow.model.Resource;
import com.resourceflow.service.RequestService;
import com.resourceflow.service.ResourceService;

import java.time.LocalDateTime;

/**
 * Utility for seeding rich, realistic sample data for demonstration and testing.
 */
public class DemoDataSeeder {

    public static void seedDemoDataIfEmpty(ResourceService resourceService, RequestService requestService) {
        if (!resourceService.getAllResources().isEmpty()) {
            return; // Data already exists
        }

        // 1. Resources
        Resource gpu = new Resource("GPU-H100", "NVIDIA H100 GPU Cluster", "Compute", 120.0, 120.0, "Hours", "High-throughput AI accelerator cluster", true);
        Resource cpu = new Resource("CPU-EPYC", "AMD EPYC Compute Nodes", "Compute", 600.0, 600.0, "Cores", "Distributed high-memory compute pool", true);
        Resource lab = new Resource("LAB-CHEM", "Spectroscopy Chemistry Lab", "Facilities", 25.0, 25.0, "Seats", "Precision laboratory instrumentation bench", true);
        Resource budget = new Resource("GRANT-USD", "Department Research Grant", "Financial", 50000.0, 50000.0, "USD", "Quarterly departmental seed funding pool", true);
        Resource gpuA100 = new Resource("GPU-A100", "NVIDIA A100 GPU Cluster", "Compute", 80.0, 80.0, "Hours", "Previous-generation accelerator cluster", true);

        resourceService.createResource(gpu);
        resourceService.createResource(gpuA100);
        resourceService.createResource(cpu);
        resourceService.createResource(lab);
        resourceService.createResource(budget);

        // 2. Competing Requests for GPU-H100 (Total demand: 40 + 50 + 60 + 30 = 180 vs Capacity 120)
        requestService.createRequest(new AllocationRequest(
                "REQ-GPU-01", "Dr. Alice (AI Vision Lab)", "GPU-H100",
                40.0, 5, 0.0, false, LocalDateTime.now().minusHours(5),
                LocalDateTime.now().plusDays(2), RequestStatus.PENDING, "Deep vision foundation model fine-tuning"
        ));

        requestService.createRequest(new AllocationRequest(
                "REQ-GPU-02", "Bob (Bioinformatics)", "GPU-H100",
                50.0, 4, 15.0, false, LocalDateTime.now().minusHours(4),
                LocalDateTime.now().plusDays(3), RequestStatus.PENDING, "Protein folding structural dynamics"
        ));

        requestService.createRequest(new AllocationRequest(
                "REQ-GPU-03", "Charlie (Quantum Simulation)", "GPU-H100",
                60.0, 3, 20.0, false, LocalDateTime.now().minusHours(3),
                LocalDateTime.now().plusDays(4), RequestStatus.PENDING, "Lattice gauge simulation"
        ));

        requestService.createRequest(new AllocationRequest(
                "REQ-GPU-04", "Dave (Robotics Club)", "GPU-H100",
                30.0, 2, 0.0, true, LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(1), RequestStatus.PENDING, "Autonomous drone simulation (Requires 100% capacity)"
        ));

        // 3. Competing Requests for CPU-EPYC (Total demand: 250 + 350 + 200 = 800 vs Capacity 600)
        requestService.createRequest(new AllocationRequest(
                "REQ-CPU-01", "Dr. Eve (Genomics Core)", "CPU-EPYC",
                250.0, 5, 50.0, false, LocalDateTime.now().minusHours(6),
                LocalDateTime.now().plusDays(5), RequestStatus.PENDING, "Whole-genome sequencing pipeline"
        ));

        requestService.createRequest(new AllocationRequest(
                "REQ-CPU-02", "Frank (Astrophysics)", "CPU-EPYC",
                350.0, 4, 100.0, false, LocalDateTime.now().minusHours(3),
                LocalDateTime.now().plusDays(3), RequestStatus.PENDING, "N-body cosmological galaxy formation"
        ));

        requestService.createRequest(new AllocationRequest(
                "REQ-CPU-03", "Grace (Stats Dept)", "CPU-EPYC",
                200.0, 2, 0.0, false, LocalDateTime.now().minusHours(2),
                null, RequestStatus.PENDING, "Monte Carlo risk model evaluation"
        ));

        // 4. Competing Requests for LAB-CHEM (Total demand: 15 + 15 = 30 vs Capacity 25)
        requestService.createRequest(new AllocationRequest(
                "REQ-LAB-01", "Prof. Heidi (Organic Synthesis)", "LAB-CHEM",
                15.0, 4, 0.0, false, LocalDateTime.now().minusHours(8),
                LocalDateTime.now().plusDays(7), RequestStatus.PENDING, "Catalytic synthesis characterization"
        ));

        // 5. Requests for GPU-A100 (Total demand: 50 + 45 = 95 vs Capacity 80).
        //    Same unit as GPU-H100, so What-If can move GPU-hours between the two clusters.
        requestService.createRequest(new AllocationRequest(
                "REQ-A100-01", "Judy (NLP Group)", "GPU-A100",
                50.0, 3, 20.0, false, LocalDateTime.now().minusHours(7),
                LocalDateTime.now().plusDays(5), RequestStatus.PENDING, "Language model evaluation runs"
        ));

        requestService.createRequest(new AllocationRequest(
                "REQ-A100-02", "Ken (Climate Modelling)", "GPU-A100",
                45.0, 4, 0.0, false, LocalDateTime.now().minusHours(2),
                LocalDateTime.now().plusDays(6), RequestStatus.PENDING, "Regional weather downscaling"
        ));

        requestService.createRequest(new AllocationRequest(
                "REQ-LAB-02", "Ivan (Materials Eng)", "LAB-CHEM",
                15.0, 3, 5.0, false, LocalDateTime.now().minusHours(2),
                LocalDateTime.now().plusDays(4), RequestStatus.PENDING, "Nanomaterial spectroscopy"
        ));
    }
}
