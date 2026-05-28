import Link from "next/link";

export default function Home() {
  return (
    <main className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto px-4 py-12">
        <h1 className="text-4xl font-bold text-gray-900 mb-4">
          Coffee Exporter EU
        </h1>
        <p className="text-xl text-gray-600 mb-8">
          Prepare coffee shipments for European buyers in days, not weeks
        </p>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white p-6 rounded-lg shadow">
            <h2 className="text-2xl font-semibold mb-3">Supplier Onboarding</h2>
            <p className="text-gray-600 mb-4">
              Add farmers, cooperatives, and washing stations with GPS coordinates
            </p>
            <Link href="/suppliers" className="text-blue-600 hover:underline">
              Get started →
            </Link>
          </div>
          
          <div className="bg-white p-6 rounded-lg shadow">
            <h2 className="text-2xl font-semibold mb-3">Shipment Readiness</h2>
            <p className="text-gray-600 mb-4">
              Track compliance checklist and readiness score
            </p>
            <Link href="/shipments" className="text-blue-600 hover:underline">
              View shipments →
            </Link>
          </div>
          
          <div className="bg-white p-6 rounded-lg shadow">
            <h2 className="text-2xl font-semibold mb-3">Export Package</h2>
            <p className="text-gray-600 mb-4">
              One-click buyer-ready export documentation
            </p>
            <Link href="/export" className="text-blue-600 hover:underline">
              Create package →
            </Link>
          </div>
        </div>
      </div>
    </main>
  );
}