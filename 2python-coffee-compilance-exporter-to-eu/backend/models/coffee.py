from sqlalchemy import Column, Integer, String, Boolean, DateTime, DECIMAL, Enum, ForeignKey, Text, JSON, Table
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from ..db.database import Base

lot_farms = Table(
    "lot_farms",
    Base.metadata,
    Column("lot_id", Integer, ForeignKey("coffee_lots.id"), primary_key=True),
    Column("farm_id", Integer, ForeignKey("farms.id"), primary_key=True),
)

class Exporter(Base):
    __tablename__ = "exporters"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    email = Column(String, nullable=False, unique=True)
    phone = Column(String, nullable=True)
    address = Column(Text, nullable=True)
    company_registration = Column(String, nullable=True)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    created_by = Column(String, nullable=True)
    
    suppliers = relationship("Supplier", back_populates="exporter")
    coffee_lots = relationship("CoffeeLot", back_populates="exporter")

class Supplier(Base):
    __tablename__ = "suppliers"
    
    id = Column(Integer, primary_key=True, index=True)
    exporter_id = Column(Integer, ForeignKey("exporters.id"), nullable=False)
    supplier_type = Column(Enum("farmer", "cooperative", "washing_station", name="supplier_type"), nullable=False)
    name = Column(String, nullable=False)
    contact_person = Column(String, nullable=True)
    phone = Column(String, nullable=True)
    email = Column(String, nullable=True)
    gps_coordinates = Column(JSON, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    created_by = Column(String, nullable=True)
    
    exporter = relationship("Exporter", back_populates="suppliers")
    farms = relationship("Farm", back_populates="supplier")

class Farm(Base):
    __tablename__ = "farms"
    
    id = Column(Integer, primary_key=True, index=True)
    supplier_id = Column(Integer, ForeignKey("suppliers.id"), nullable=False)
    farm_name = Column(String, nullable=True)
    farm_code = Column(String, nullable=True)
    boundary_polygon = Column(JSON, nullable=True)
    elevation = Column(String, nullable=True)
    variety = Column(String, nullable=True)
    harvest_period = Column(String, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    created_by = Column(String, nullable=True)
    
    supplier = relationship("Supplier", back_populates="farms")
    lots = relationship("CoffeeLot", secondary=lot_farms, back_populates="farms")

class CoffeeLot(Base):
    __tablename__ = "coffee_lots"
    
    id = Column(Integer, primary_key=True, index=True)
    exporter_id = Column(Integer, ForeignKey("exporters.id"), nullable=False)
    lot_id = Column(String, nullable=False)
    name = Column(String, nullable=True)
    origin_region = Column(String, nullable=True)
    weight_kg = Column(DECIMAL, nullable=True)
    harvest_start = Column(DateTime, nullable=True)
    harvest_end = Column(DateTime, nullable=True)
    status = Column(Enum("registered", "gps_uploaded", "traceability_complete", "ready", "shipped", name="lot_status"), default="registered")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    created_by = Column(String, nullable=True)
    
    exporter = relationship("Exporter", back_populates="coffee_lots")
    shipments = relationship("Shipment", back_populates="coffee_lot")
    farms = relationship("Farm", secondary=lot_farms, back_populates="lots")
    documents = relationship("Document", back_populates="lot")

class Shipment(Base):
    __tablename__ = "shipments"
    
    id = Column(Integer, primary_key=True, index=True)
    coffee_lot_id = Column(Integer, ForeignKey("coffee_lots.id"), nullable=False)
    shipment_code = Column(String, nullable=False, unique=True)
    buyer_name = Column(String, nullable=True)
    destination_port = Column(String, nullable=True)
    departure_date = Column(DateTime, nullable=True)
    arrival_date = Column(DateTime, nullable=True)
    status = Column(Enum("draft", "ready", "shipped", "delivered", name="shipment_status"), default="draft")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    created_by = Column(String, nullable=True)
    
    coffee_lot = relationship("CoffeeLot", back_populates="shipments")
    documents = relationship("Document", back_populates="shipment")

class Document(Base):
    __tablename__ = "documents"
    
    id = Column(Integer, primary_key=True, index=True)
    documentable_type = Column(Enum("farm", "lot", "shipment", "supplier", name="documentable_type"), nullable=False)
    documentable_id = Column(Integer, nullable=False)
    document_type = Column(Enum("certificate", "invoice", "legal", "traceability", "other", name="document_type"), nullable=False)
    file_path = Column(String, nullable=False)
    original_filename = Column(String, nullable=True)
    uploaded_by = Column(String, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    farm = relationship("Farm", back_populates="documents")
    lot = relationship("CoffeeLot", back_populates="documents")
    shipment = relationship("Shipment", back_populates="documents")
    supplier = relationship("Supplier", back_populates="documents")