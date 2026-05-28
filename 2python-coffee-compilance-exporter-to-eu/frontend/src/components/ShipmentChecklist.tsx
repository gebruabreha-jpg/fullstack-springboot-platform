import { CheckCircleIcon, ExclamationCircleIcon } from "@heroicons/react/24/outline";

interface ShipmentChecklistProps {
  shipmentId: string;
  readiness: number;
  checks: {
    label: string;
    status: "complete" | "missing" | "warning";
  }[];
}

export default function ShipmentChecklist({ shipmentId, readiness, checks }: ShipmentChecklistProps) {
  return (
    <div className="bg-white rounded-lg shadow p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-semibold">{shipmentId}</h2>
        <div className="text-right">
          <p className="text-sm text-gray-500">EU Buyer Readiness</p>
          <p className="text-3xl font-bold">{readiness}%</p>
        </div>
      </div>
      
      <div className="space-y-3">
        {checks.map((check, index) => (
          <div key={index} className="flex items-center gap-3">
            {check.status === "complete" ? (
              <CheckCircleIcon className="h-5 w-5 text-green-500" />
            ) : check.status === "warning" ? (
              <div className="h-5 w-5 rounded-full bg-yellow-400" />
            ) : (
              <ExclamationCircleIcon className="h-5 w-5 text-red-500" />
            )}
            <span className={check.status === "missing" ? "text-red-700" : "text-gray-700"}>
              {check.label}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}