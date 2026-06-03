package si.um.feri.smartjobs.salary.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Salary prediction returned by the salary service.")
public record SalaryPredictionResponse(
        @Schema(description = "Whether salary prediction is available for the provided criteria.", example = "true")
        Boolean available,
        @Schema(description = "Predicted minimum salary.", example = "2800")
        BigDecimal predictedMinSalary,
        @Schema(description = "Predicted maximum salary.", example = "4300")
        BigDecimal predictedMaxSalary,
        @Schema(description = "Salary currency.", example = "EUR")
        String currency,
        @Schema(description = "Completeness score for the profile used in prediction.", example = "86")
        Integer profileCompleteness,
        @Schema(description = "Number of similar jobs used for market comparison.", example = "37")
        Integer similarJobs,
        @Schema(description = "Market used for prediction.", example = "Slovenia")
        String market,
        @Schema(description = "Whether the market had to be assumed by the service.", example = "false")
        Boolean marketAssumed,
        @Schema(description = "Human-readable prediction message.", example = "Prediction based on similar backend developer jobs in Slovenia.")
        String message,
        @Schema(description = "Model mean absolute error, if available.", example = "320")
        BigDecimal modelMae
) {
}
